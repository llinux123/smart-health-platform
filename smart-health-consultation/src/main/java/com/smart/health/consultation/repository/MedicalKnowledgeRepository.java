package com.smart.health.consultation.repository;

import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 医学知识库 ES Repository
 */
public interface MedicalKnowledgeRepository extends ElasticsearchRepository<MedicalKnowledgeDocument, String> {
}
