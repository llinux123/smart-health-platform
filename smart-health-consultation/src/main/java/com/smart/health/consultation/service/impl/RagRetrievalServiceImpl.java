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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final ElasticsearchOperations elasticsearchOperations;

    private static final int SNIPPET_MAX_LENGTH = 200;

    @Override
    public List<String> retrieve(String query, int topK) {
        return retrieveDocuments(query, topK).stream()
                .map(doc -> String.format("《%s》(%s): %s", doc.getTitle(), doc.getCategory(), doc.getContent()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ConsultStreamResponse.Citation> retrieveCitations(String query, int topK) {
        return retrieveDocuments(query, topK).stream()
                .map(doc -> ConsultStreamResponse.Citation.builder()
                        .title(doc.getTitle())
                        .category(doc.getCategory())
                        .snippet(truncateSnippet(doc.getContent()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<MedicalKnowledgeDocument> retrieveDocuments(String query, int topK) {
        try {
            // 使用 ES Java Client 类型化 API 构建 multi_match 查询
            Query multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields("title^2", "content")
                    .fuzziness("AUTO")
            )._toQuery();

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(multiMatchQuery)
                    .withMaxResults(topK)
                    .build();

            SearchHits<MedicalKnowledgeDocument> searchHits =
                    elasticsearchOperations.search(nativeQuery, MedicalKnowledgeDocument.class);

            if (searchHits.isEmpty()) {
                log.debug("RAG 检索无结果, query={}", query);
                return Collections.emptyList();
            }

            List<MedicalKnowledgeDocument> results = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("RAG 检索完成, query={}, 命中 {} 条文档", query, results.size());
            return results;

        } catch (Exception e) {
            log.error("RAG 检索失败, query={}: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String truncateSnippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= SNIPPET_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, SNIPPET_MAX_LENGTH) + "...";
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
}
