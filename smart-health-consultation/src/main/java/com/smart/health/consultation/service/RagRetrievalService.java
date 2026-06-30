package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.ConsultStreamResponse;

import java.util.List;

/**
 * RAG 检索服务接口
 * 从 ES 医学知识库中检索相关文档，用于增强 AI 回答
 */
public interface RagRetrievalService {

    /**
     * 检索相关医学知识
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 检索到的知识文档内容列表
     */
    List<String> retrieve(String query, int topK);

    /**
     * 检索 RAG 引用来源
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 引用来源列表
     */
    List<ConsultStreamResponse.Citation> retrieveCitations(String query, int topK);

    /**
     * 检索并拼接为上下文字符串
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 拼接后的知识上下文字符串
     */
    String retrieveAsContext(String query, int topK);
}
