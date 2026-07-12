package com.smart.health.consultation.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ScriptScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.PageResult;
import com.smart.health.common.result.ResultCode;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import com.smart.health.consultation.service.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 检索服务实现
 * 支持三种检索模式：BM25 文本检索、向量检索（cosine）、混合检索（BM25 + 向量）
 * 默认使用混合检索，Embedding 不可用时自动降级为纯 BM25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private static final String INDEX_NAME = "idx_medical_knowledge";

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient esClient;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Override
    public List<String> retrieve(String query, int topK) {
        SearchHits<MedicalKnowledgeDocument> searchHits = searchDocuments(query, topK);
        if (searchHits == null || searchHits.isEmpty()) {
            log.debug("RAG 检索无结果, query={}", query);
            return Collections.emptyList();
        }

        List<String> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> String.format("《%s》(%s): %s", doc.getTitle(), doc.getCategory(), doc.getContent()))
                .collect(Collectors.toList());

        log.info("RAG 检索完成, query={}, 命中 {} 条文档", query, results.size());
        return results;
    }

    @Override
    public String retrieveAsContext(String query, int topK) {
        List<String> docs = retrieve(query, topK);
        if (docs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            sb.append("【知识").append(i + 1).append("】\n");
            sb.append(docs.get(i)).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    public List<ConsultStreamResponse.Citation> retrieveCitations(String query, int topK) {
        SearchHits<MedicalKnowledgeDocument> searchHits = searchDocuments(query, topK);
        if (searchHits == null || searchHits.isEmpty()) {
            return Collections.emptyList();
        }

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> ConsultStreamResponse.Citation.builder()
                        .title(doc.getTitle())
                        .category(doc.getCategory())
                        .snippet(truncate(doc.getContent(), 120))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 执行 ES 检索：优先使用混合检索（BM25 + 向量），降级为纯 BM25
     */
    private SearchHits<MedicalKnowledgeDocument> searchDocuments(String query, int topK) {
        try {
            // 尝试混合检索（需要 Embedding 可用）
            if (embeddingModel != null) {
                try {
                    float[] rawEmbedding = embeddingModel.embed(query);
                    List<Float> floatEmbedding = new java.util.ArrayList<>(rawEmbedding.length);
                    for (float v : rawEmbedding) floatEmbedding.add(v);
                    return hybridSearch(query, floatEmbedding, topK);
                } catch (Exception e) {
                    log.warn("Embedding 调用失败，降级为纯 BM25 检索: {}", e.getMessage());
                }
            }

            // 降级：纯 BM25 文本检索
            return bm25Search(query, topK);
        } catch (Exception e) {
            log.error("RAG 检索失败, query={}: {}", query, e.getMessage());
            return null;
        }
    }

    /**
     * BM25 文本检索
     */
    private SearchHits<MedicalKnowledgeDocument> bm25Search(String query, int topK) {
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(query)
                .fields("title^2", "content")
                .fuzziness("AUTO")
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(multiMatchQuery)
                .withMaxResults(topK)
                .build();

        return elasticsearchOperations.search(nativeQuery, MedicalKnowledgeDocument.class);
    }

    /**
     * 混合检索：BM25 + 向量（script_score cosine），通过 bool query 组合
     */
    private SearchHits<MedicalKnowledgeDocument> hybridSearch(String textQuery, List<Float> queryVector, int topK) {
        // BM25 子查询
        Query bm25Query = MultiMatchQuery.of(m -> m
                .query(textQuery)
                .fields("title^2", "content")
                .fuzziness("AUTO")
                .boost(1.0f)
        )._toQuery();

        // 向量检索子查询（使用 script_score + cosineSimilarity）
        Query vectorQuery = ScriptScoreQuery.of(ss -> ss
                .minScore(0.5f)
                .query(Query.of(q -> q.matchAll(ma -> ma)))
                .script(s -> s
                        .inline(i -> i
                                .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                .params("query_vector", co.elastic.clients.json.JsonData.of(queryVector))))
                .boost(0.8f)
        )._toQuery();

        // 组合 bool query
        Query combinedQuery = BoolQuery.of(b -> b
                .should(bm25Query)
                .should(vectorQuery)
        )._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(combinedQuery)
                .withMaxResults(topK)
                .build();

        return elasticsearchOperations.search(nativeQuery, MedicalKnowledgeDocument.class);
    }

    /**
     * 截断字符串，超出部分加省略号
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    @Override
    public int importDocument(String title, String content, String category) {
        try {
            Date now = new Date();
            MedicalKnowledgeDocument doc = MedicalKnowledgeDocument.builder()
                    .title(title)
                    .content(content)
                    .category(category)
                    .updateTime(now)
                    .build();

            // 生成 embedding 向量
            if (embeddingModel != null) {
                try {
                    float[] rawEmbedding = embeddingModel.embed(title + " " + content);
                    List<Float> floatList = new java.util.ArrayList<>(rawEmbedding.length);
                    for (float v : rawEmbedding) floatList.add(v);
                    doc.setEmbedding(floatList);
                } catch (Exception e) {
                    log.warn("导入文档 Embedding 生成失败: {}", e.getMessage());
                }
            }

            esClient.index(IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .document(doc)));
            esClient.indices().refresh(r -> r.index(INDEX_NAME));

            log.info("导入医学知识文档成功, title={}, category={}", title, category);
            return 1;
        } catch (Exception e) {
            log.error("导入医学知识文档失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 分页查询知识库文档：支持关键字搜索 + 分类过滤，按更新时间倒序，_id 二级排序
     */
    @Override
    public PageResult<MedicalKnowledgeDocument> listDocuments(int page, int size, String keyword, String category) {
        try {
            int from = (page - 1) * size;

            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .from(from)
                    .size(size)
                    .sort(so -> so.field(f -> f.field("updateTime").order(SortOrder.Desc)))
                    .trackTotalHits(t -> t.enabled(true));

            // 构造 query：分类过滤 + 关键字搜索
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            boolean hasCondition = false;
            if (category != null && !category.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("category").value(category)))));
                hasCondition = true;
            }
            if (keyword != null && !keyword.isBlank()) {
                boolBuilder.must(Query.of(q -> q.multiMatch(mm -> mm
                        .query(keyword)
                        .fields("title^2", "content")
                        .fuzziness("AUTO"))));
                hasCondition = true;
            }
            if (hasCondition) {
                builder.query(q -> q.bool(boolBuilder.build()));
            } else {
                builder.query(q -> q.matchAll(ma -> ma));
            }

            SearchResponse<MedicalKnowledgeDocument> response = esClient.search(builder.build(), MedicalKnowledgeDocument.class);

            List<MedicalKnowledgeDocument> docs = response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .peek(hit -> hit.source().setId(hit.id()))
                    .map(Hit::source)
                    .map(this::stripEmbedding)
                    .collect(Collectors.toList());

            long total = 0;
            TotalHits totalHits = response.hits().total();
            if (totalHits != null) {
                total = totalHits.value();
            }

            return PageResult.of(docs, total, page, size);
        } catch (Exception e) {
            log.error("查询知识库文档失败: {}", e.getMessage());
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }
    }

    /**
     * 详情查询：按 ES 文档 _id
     */
    @Override
    public MedicalKnowledgeDocument getDocument(String id) {
        try {
            GetResponse<MedicalKnowledgeDocument> resp = esClient.get(
                    GetRequest.of(g -> g.index(INDEX_NAME).id(id)),
                    MedicalKnowledgeDocument.class);
            if (!resp.found() || resp.source() == null) {
                return null;
            }
            MedicalKnowledgeDocument doc = resp.source();
            doc.setId(resp.id());
            return stripEmbedding(doc);
        } catch (Exception e) {
            log.error("获取知识库文档详情失败, id={}: {}", id, e.getMessage());
            return null;
        }
    }

    /**
     * 更新知识库文档（全量覆盖 title/content/category）
     * 当 title 或 content 变化时重新生成 embedding；重嵌失败抛出 BusinessException
     */
    @Override
    public MedicalKnowledgeDocument updateDocument(String id, String title, String content, String category) {
        try {
            MedicalKnowledgeDocument existing = getDocument(id);
            if (existing == null) {
                return null;
            }

            // 判断是否需要重嵌（title 或 content 变化才重嵌）
            boolean needReEmbed = embeddingModel != null
                    && (!equalsNullable(title, existing.getTitle())
                    || !equalsNullable(content, existing.getContent()));

            existing.setTitle(title);
            existing.setContent(content);
            existing.setCategory(category);
            existing.setUpdateTime(new Date());

            if (needReEmbed) {
                try {
                    float[] rawEmbedding = embeddingModel.embed(title + " " + content);
                    List<Float> floatList = new java.util.ArrayList<>(rawEmbedding.length);
                    for (float v : rawEmbedding) floatList.add(v);
                    existing.setEmbedding(floatList);
                } catch (Exception e) {
                    log.error("更新文档 Embedding 重建失败, id={}: {}", id, e.getMessage());
                    throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE,
                            "知识库向量重建失败，请稍后重试");
                }
            }

            esClient.index(IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(id)
                    .document(existing)));
            esClient.indices().refresh(r -> r.index(INDEX_NAME));

            log.info("更新知识库文档成功, id={}, title={}, category={}, reEmbed={}",
                    id, title, category, needReEmbed);
            return stripEmbedding(existing);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新知识库文档失败, id={}: {}", id, e.getMessage());
            throw new BusinessException("更新知识库文档失败: " + e.getMessage());
        }
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 列表 / 详情不返回 embedding 向量（1024 维 = ~4KB，没必要传输给后台）
     */
    private MedicalKnowledgeDocument stripEmbedding(MedicalKnowledgeDocument doc) {
        if (doc != null) {
            doc.setEmbedding(null);
        }
        return doc;
    }

    /**
     * 根据 ES 文档 ID 删除知识库文档
     */
    @Override
    public boolean deleteDocument(String id) {
        try {
            esClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(id));
            esClient.indices().refresh(r -> r.index(INDEX_NAME));
            log.info("删除知识库文档成功, id={}", id);
            return true;
        } catch (Exception e) {
            log.error("删除知识库文档失败, id={}, error={}", id, e.getMessage());
            return false;
        }
    }

    /**
     * 从 ES 聚合出所有出现过的 category，去重后按字典序返回
     */
    @Override
    public List<String> listCategories() {
        try {
            SearchRequest req = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .size(0)
                    .aggregations("categories", Aggregation.of(a -> a
                            .terms(t -> t.field("category").size(1000)))));
            SearchResponse<Void> response = esClient.search(req, Void.class);
            List<StringTermsBucket> buckets = response.aggregations()
                    .get("categories")
                    .sterms()
                    .buckets()
                    .array();
            if (buckets == null || buckets.isEmpty()) {
                return Collections.emptyList();
            }
            return buckets.stream()
                    .map(StringTermsBucket::key)
                    .map(co.elastic.clients.elasticsearch._types.FieldValue::stringValue)
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询知识库分类列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}