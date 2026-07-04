package com.smart.health.consultation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import com.smart.health.consultation.service.impl.RagRetrievalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagRetrievalServiceImpl 单元测试")
class RagRetrievalServiceImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchClient esClient;

    @Mock
    private EmbeddingModel embeddingModel;

    private RagRetrievalServiceImpl ragRetrievalService;

    @SuppressWarnings("unchecked")
    private <T> SearchHits<T> mockSearchHits(T content) {
        SearchHit<T> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(content);

        SearchHits<T> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        return searchHits;
    }

    @BeforeEach
    void setUp() {
        ragRetrievalService = new RagRetrievalServiceImpl(elasticsearchOperations, esClient);
        try {
            var field = RagRetrievalServiceImpl.class.getDeclaredField("embeddingModel");
            field.setAccessible(true);
            field.set(ragRetrievalService, embeddingModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("retrieve - 混合检索成功返回结果")
    void retrieve_hybridSearch_success() throws Exception {
        // Given
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed(anyString())).thenReturn(embedding);

        MedicalKnowledgeDocument doc = MedicalKnowledgeDocument.builder()
                .docId("MED-001").title("测试文档").category("测试科")
                .content("测试内容").build();
        SearchHits<MedicalKnowledgeDocument> searchHits = mockSearchHits(doc);

        when(elasticsearchOperations.search(any(Query.class), eq(MedicalKnowledgeDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> results = ragRetrievalService.retrieve("头痛", 3);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("测试文档");
        verify(embeddingModel).embed("头痛");
    }

    @Test
    @DisplayName("retrieve - Embedding 失败降级为 BM25")
    void retrieve_embeddingFails_fallbackToBm25() throws Exception {
        // Given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("Embedding API unavailable"));

        MedicalKnowledgeDocument doc = MedicalKnowledgeDocument.builder()
                .docId("MED-002").title("BM25文档").category("内科")
                .content("BM25内容").build();
        SearchHits<MedicalKnowledgeDocument> searchHits = mockSearchHits(doc);

        when(elasticsearchOperations.search(any(Query.class), eq(MedicalKnowledgeDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> results = ragRetrievalService.retrieve("发热", 3);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains("BM25文档");
    }

    @Test
    @DisplayName("retrieve - ES 异常返回空列表")
    void retrieve_esException_returnsEmpty() throws Exception {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        when(elasticsearchOperations.search(any(Query.class), eq(MedicalKnowledgeDocument.class)))
                .thenThrow(new RuntimeException("ES connection refused"));

        // When
        List<String> results = ragRetrievalService.retrieve("咳嗽", 3);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("retrieveAsContext - 空结果返回空字符串")
    void retrieveAsContext_empty_returnsEmptyString() throws Exception {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f});
        when(elasticsearchOperations.search(any(Query.class), eq(MedicalKnowledgeDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        // When
        String context = ragRetrievalService.retrieveAsContext("腹痛", 3);

        // Then
        assertThat(context).isEmpty();
    }

    @Test
    @DisplayName("importDocument - 成功导入文档")
    void importDocument_success() throws Exception {
        // Given
        IndexResponse response = mock(IndexResponse.class);
        when(esClient.index(any(IndexRequest.class))).thenReturn(response);

        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(esClient.indices()).thenReturn(indicesClient);

        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // When
        int count = ragRetrievalService.importDocument("测试标题", "测试内容", "测试科");

        // Then
        assertThat(count).isEqualTo(1);
        verify(esClient).index(any(IndexRequest.class));
    }

    @Test
    @DisplayName("importDocument - ES 异常返回 0")
    void importDocument_esFails_returnsZero() throws Exception {
        // Given
        when(esClient.index(any(IndexRequest.class))).thenThrow(new RuntimeException("ES write failed"));

        // When
        int count = ragRetrievalService.importDocument("标题", "内容", "科");

        // Then
        assertThat(count).isEqualTo(0);
    }
}
