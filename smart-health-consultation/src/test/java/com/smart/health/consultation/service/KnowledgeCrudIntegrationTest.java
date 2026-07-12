package com.smart.health.consultation.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 知识库 CRUD 集成测试（需要运行中的 ES 实例）
 * 测试前请确保 ES 运行在 localhost:9200
 */
@Tag("integration")
@SpringBootTest(properties = {
        "knowledge.initializer.enabled=false",
        "spring.ai.openai.api-key=test-key",
        "spring.elasticsearch.uris=http://localhost:9200"
})
@DisplayName("知识库 CRUD 集成测试")
class KnowledgeCrudIntegrationTest {

    private static final String TEST_TITLE = "集成测试文档";
    private static final String TEST_CONTENT = "这是一条集成测试的医学知识内容";
    private static final String TEST_CATEGORY = "测试科";

    @Autowired
    private RagRetrievalService ragRetrievalService;

    @Autowired
    private ElasticsearchClient esClient;

    @BeforeEach
    void setUp() throws Exception {
        // 清理测试数据
        try {
            PageResult<MedicalKnowledgeDocument> existing = ragRetrievalService.listDocuments(1, 100, null, null);
            for (MedicalKnowledgeDocument doc : existing.getList()) {
                if (TEST_TITLE.equals(doc.getTitle())) {
                    ragRetrievalService.deleteDocument(doc.getId());
                }
            }
            esClient.indices().refresh(r -> r.index("idx_medical_knowledge"));
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("导入 + 详情 + 更新 + 删除 全流程")
    void crudFullFlow() {
        // 1. 导入
        int count = ragRetrievalService.importDocument(TEST_TITLE, TEST_CONTENT, TEST_CATEGORY);
        assertThat(count).isEqualTo(1);

        // 2. 列表分页查询（验证导入成功）
        PageResult<MedicalKnowledgeDocument> page = ragRetrievalService.listDocuments(1, 100, null, null);
        assertThat(page.getList()).isNotEmpty();

        MedicalKnowledgeDocument found = page.getList().stream()
                .filter(d -> TEST_TITLE.equals(d.getTitle()))
                .findFirst().orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo(TEST_TITLE);
        assertThat(found.getCategory()).isEqualTo(TEST_CATEGORY);
        assertThat(found.getContent()).isEqualTo(TEST_CONTENT);
        assertThat(found.getEmbedding()).isNull(); // 列表不返回 embedding
        String id = found.getId();
        assertThat(id).isNotNull();

        // 3. 详情查询
        MedicalKnowledgeDocument detail = ragRetrievalService.getDocument(id);
        assertThat(detail).isNotNull();
        assertThat(detail.getTitle()).isEqualTo(TEST_TITLE);
        assertThat(detail.getContent()).isEqualTo(TEST_CONTENT);

        // 4. 更新
        String newTitle = TEST_TITLE + "-已更新";
        String newContent = TEST_CONTENT + "-已更新";
        MedicalKnowledgeDocument updated = ragRetrievalService.updateDocument(
                id, newTitle, newContent, TEST_CATEGORY);
        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo(newTitle);
        assertThat(updated.getContent()).isEqualTo(newContent);

        // 5. 验证更新后
        MedicalKnowledgeDocument afterUpdate = ragRetrievalService.getDocument(id);
        assertThat(afterUpdate.getTitle()).isEqualTo(newTitle);

        // 6. 删除
        boolean deleted = ragRetrievalService.deleteDocument(id);
        assertThat(deleted).isTrue();

        // 7. 验证删除后不存在
        MedicalKnowledgeDocument afterDelete = ragRetrievalService.getDocument(id);
        assertThat(afterDelete).isNull();
    }

    @Test
    @DisplayName("分类聚合 - 返回去重列表")
    void listCategories() {
        // 先导入一条以保证至少有分类
        ragRetrievalService.importDocument("分类测试", "内容", TEST_CATEGORY);
        try {
            List<String> cats = ragRetrievalService.listCategories();
            assertThat(cats).isNotNull();
            assertThat(cats).contains(TEST_CATEGORY);
            // 包含内置种子分类
            assertThat(cats).containsAnyOf("皮肤科", "内科");
        } finally {
            PageResult<MedicalKnowledgeDocument> page = ragRetrievalService.listDocuments(1, 100, null, null);
            page.getList().stream()
                    .filter(d -> "分类测试".equals(d.getTitle()))
                    .forEach(d -> ragRetrievalService.deleteDocument(d.getId()));
        }
    }

    @Test
    @DisplayName("关键字搜索")
    void searchByKeyword() {
        ragRetrievalService.importDocument("唯一搜索目标", "此内容包含搜索关键词", "测试科");
        try {
            PageResult<MedicalKnowledgeDocument> result = ragRetrievalService.listDocuments(1, 10, "搜索关键词", null);
            assertThat(result.getList()).isNotEmpty();
            assertThat(result.getList()).anyMatch(d -> "唯一搜索目标".equals(d.getTitle()));
        } finally {
            PageResult<MedicalKnowledgeDocument> page = ragRetrievalService.listDocuments(1, 100, null, null);
            page.getList().stream()
                    .filter(d -> "唯一搜索目标".equals(d.getTitle()))
                    .forEach(d -> ragRetrievalService.deleteDocument(d.getId()));
        }
    }

    @Test
    @DisplayName("分类过滤")
    void filterByCategory() {
        ragRetrievalService.importDocument("过滤测试", "内容", "专属分类ABCDE");
        try {
            PageResult<MedicalKnowledgeDocument> result = ragRetrievalService.listDocuments(1, 10, null, "专属分类ABCDE");
            assertThat(result.getList()).isNotEmpty();
            assertThat(result.getList()).allMatch(d -> "专属分类ABCDE".equals(d.getCategory()));
        } finally {
            PageResult<MedicalKnowledgeDocument> page = ragRetrievalService.listDocuments(1, 100, null, null);
            page.getList().stream()
                    .filter(d -> "过滤测试".equals(d.getTitle()))
                    .forEach(d -> ragRetrievalService.deleteDocument(d.getId()));
        }
    }
}