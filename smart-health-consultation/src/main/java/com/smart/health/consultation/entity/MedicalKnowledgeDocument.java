package com.smart.health.consultation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * 医学知识库 ES 文档实体
 * 对应索引: idx_medical_knowledge
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "idx_medical_knowledge")
public class MedicalKnowledgeDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String docId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Date)
    private Date updateTime;

    /** 1024 维 Embedding 向量（由 EmbeddingModel 生成，用于向量检索） */
    private List<Float> embedding;
}