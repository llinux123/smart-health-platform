package com.smart.health.consultation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
}
