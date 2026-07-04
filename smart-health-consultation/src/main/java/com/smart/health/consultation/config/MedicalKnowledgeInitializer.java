package com.smart.health.consultation.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 医学知识库初始化器
 * 启动时创建 ES 索引（ik 分词 + dense_vector mapping）并从外部 JSON 文件加载医学知识文档
 * 种子数据来源由 knowledge.seed.location 配置，默认 classpath:knowledge/medical-knowledge-seed.json
 * 可通过 knowledge.seed.enabled=false 关闭种子数据加载
 * 直接使用 ElasticsearchClient (ES Java Client) 避免 Spring Data ES 多模块严格模式问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalKnowledgeInitializer implements ApplicationRunner {

    private static final String INDEX_NAME = "idx_medical_knowledge";
    private static final int EMBEDDING_DIMS = 1536;

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Value("${knowledge.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${knowledge.seed.location:classpath:knowledge/medical-knowledge-seed.json}")
    private Resource seedLocation;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                createIndexWithMapping();
                log.info("创建 ES 索引（含 mapping）: {}", INDEX_NAME);
            }

            long count = esClient.count(CountRequest.of(c -> c.index(INDEX_NAME))).count();
            if (count > 0) {
                log.info("医学知识库已有数据（{}条），跳过初始化", count);
                return;
            }

            List<MedicalKnowledgeDocument> docs = seedEnabled
                    ? loadDocumentsFromJson()
                    : Collections.emptyList();
            if (docs.isEmpty()) {
                log.info("种子数据为空或已禁用（knowledge.seed.enabled=false），跳过文档导入");
                return;
            }
            for (MedicalKnowledgeDocument doc : docs) {
                // 生成 embedding 向量
                if (embeddingModel != null) {
                    try {
                        float[] rawEmbedding = embeddingModel.embed(doc.getTitle() + " " + doc.getContent());
                        List<Float> floatEmbedding = new java.util.ArrayList<>(rawEmbedding.length);
                        for (float v : rawEmbedding) floatEmbedding.add(v);
                        doc.setEmbedding(floatEmbedding);
                    } catch (Exception e) {
                        log.warn("Embedding 生成失败（文档 {}），跳过向量写入: {}", doc.getDocId(), e.getMessage());
                    }
                }
                esClient.index(IndexRequest.of(i -> i.index(INDEX_NAME).document(doc)));
            }

            esClient.indices().refresh(r -> r.index(INDEX_NAME));
            log.info("医学知识库初始化完成，共导入 {} 条知识文档（embedding={})",
                    docs.size(), embeddingModel != null ? "已启用" : "未启用");

        } catch (Exception e) {
            log.warn("医学知识库初始化失败（ES可能未就绪），RAG检索将不可用: {}", e.getMessage());
        }
    }

    /**
     * 创建索引并指定 mapping（ik 分词器 + dense_vector）
     * 若 ik 不可用则降级为 standard 分词器
     */
    private void createIndexWithMapping() {
        String analyzer = detectIkAnalyzer();
        try {
            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m
                            .properties("docId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t
                                    .analyzer(analyzer)
                                    .searchAnalyzer("ik_smart".equals(analyzer) ? "ik_smart" : "standard")))
                            .properties("content", p -> p.text(t -> t
                                    .analyzer(analyzer)
                                    .searchAnalyzer("ik_smart".equals(analyzer) ? "ik_smart" : "standard")))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("embedding", p -> p.denseVector(dv -> dv
                                    .dims(EMBEDDING_DIMS)
                                    .index(true)
                                    .similarity("cosine"))))
            ));
        } catch (Exception e) {
            log.error("创建索引 mapping 失败: {}", e.getMessage());
            throw new RuntimeException("创建索引 mapping 失败", e);
        }
    }

    /**
     * 检测 ik 分词器是否可用，不可用则降级为 standard
     */
    private String detectIkAnalyzer() {
        try {
            esClient.indices().analyze(a -> a
                    .analyzer("ik_max_word")
                    .text("测试分词"));
            log.info("检测到 ik 分词器可用，使用 ik_max_word");
            return "ik_max_word";
        } catch (Exception e) {
            log.warn("ik 分词器不可用，降级使用 standard 分词器（生产环境请安装 analysis-ik 插件）");
            return "standard";
        }
    }

    /**
     * 从 JSON 文件加载种子文档。
     * 支持 classpath: 前缀和文件系统路径。
     */
    private List<MedicalKnowledgeDocument> loadDocumentsFromJson() {
        try {
            if (!seedLocation.exists()) {
                log.warn("种子数据文件不存在: {}，跳过加载", seedLocation.getDescription());
                return Collections.emptyList();
            }
            try (InputStream is = seedLocation.getInputStream()) {
                List<MedicalKnowledgeDocument> docs = objectMapper.readValue(
                        is, new TypeReference<List<MedicalKnowledgeDocument>>() {});
                log.info("从 {} 加载了 {} 条种子文档", seedLocation.getDescription(), docs.size());
                return docs;
            }
        } catch (IOException e) {
            log.warn("种子数据文件读取失败: {}，跳过加载", e.getMessage());
            return Collections.emptyList();
        }
    }
}
