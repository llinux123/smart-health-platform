package com.smart.health.consultation.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import com.smart.health.consultation.service.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 检索服务实现
 * 使用 Elasticsearch 进行文本检索（BM25），从医学知识库中检索相关文档
 * 注：向量检索需要 EmbeddingModel 支持，当前仅使用文本检索，后续可扩展
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final ElasticsearchOperations elasticsearchOperations;

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
     * 执行 ES 检索，统一供 retrieve / retrieveAsContext / retrieveCitations 复用
     */
    private SearchHits<MedicalKnowledgeDocument> searchDocuments(String query, int topK) {
        try {
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
        } catch (Exception e) {
            log.error("RAG 检索失败, query={}: {}", query, e.getMessage());
            return null;
        }
    }

    /**
     * 截断字符串，超出部分加省略号
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
