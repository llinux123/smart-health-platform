package com.smart.health.consultation.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ScriptScoreQuery;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.smart.health.common.result.PageResult;
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
            MedicalKnowledgeDocument doc = MedicalKnowledgeDocument.builder()
                    .title(title)
                    .content(content)
                    .category(category)
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
     * 分页查询知识库文档，支持关键字搜索
     */
    @Override
    public PageResult<MedicalKnowledgeDocument> listDocuments(int page, int size, String keyword) {
        try {
            int from = (page - 1) * size;

            SearchRequest.Builder builder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .from(from)
                    .size(size)
                    .sort(so -> so.field(f -> f.field("category").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)));

            if (keyword != null && !keyword.isBlank()) {
                builder.query(q -> q
                        .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("title^2", "content")
                                .fuzziness("AUTO")));
            } else {
                builder.query(q -> q.matchAll(ma -> ma));
            }

            SearchResponse<MedicalKnowledgeDocument> response = esClient.search(builder.build(), MedicalKnowledgeDocument.class);

            List<MedicalKnowledgeDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(java.util.Objects::nonNull)
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
}
