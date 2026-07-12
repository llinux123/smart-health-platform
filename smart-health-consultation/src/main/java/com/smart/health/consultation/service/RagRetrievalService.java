package com.smart.health.consultation.service;

import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;

import java.util.List;

/**
 * RAG 检索服务接口
 * 从 ES 医学知识库中检索相关文档，用于增强 AI 回答
 */
public interface RagRetrievalService {

    /**
     * 检索相关医学知识（返回结构化引用信息）
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 检索到的知识文档内容列表（用于拼入 Prompt）
     */
    List<String> retrieve(String query, int topK);

    /**
     * 检索并拼接为上下文字符串（用于注入 Prompt）
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 拼接后的知识上下文字符串
     */
    String retrieveAsContext(String query, int topK);

    /**
     * 检索并返回引用来源列表（用于 SSE 响应中的 citations 字段）
     *
     * @param query 用户问题
     * @param topK  返回最相关的文档数量
     * @return 引用来源列表
     */
    List<ConsultStreamResponse.Citation> retrieveCitations(String query, int topK);

    /**
     * 导入医学知识文档到 ES 知识库
     *
     * @param title    文档标题
     * @param content  文档内容
     * @param category 分类（科室）
     * @return 导入成功条数
     */
    int importDocument(String title, String content, String category);

    /**
     * 分页查询知识库文档，支持关键字搜索 + 分类过滤，按更新时间倒序
     *
     * @param page     页码（从1开始）
     * @param size     每页大小
     * @param keyword  搜索关键字（可选，匹配标题或内容）
     * @param category 分类过滤（可选）
     * @return 分页结果
     */
    PageResult<MedicalKnowledgeDocument> listDocuments(int page, int size, String keyword, String category);

    /**
     * 根据 ES 文档 ID 获取完整知识库文档（详情）
     *
     * @param id ES 文档 ID
     * @return 完整文档，null 表示不存在
     */
    MedicalKnowledgeDocument getDocument(String id);

    /**
     * 更新知识库文档（全量覆盖）
     * 当 title 或 content 变化时重新生成 embedding；重嵌失败抛出 BusinessException
     *
     * @param id       ES 文档 ID
     * @param title    新标题
     * @param content  新内容
     * @param category 新分类
     * @return 更新后的完整文档
     */
    MedicalKnowledgeDocument updateDocument(String id, String title, String content, String category);

    /**
     * 根据 ES 文档 ID 删除知识库文档
     *
     * @param id ES 文档 ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String id);

    /**
     * 获取知识库中已存在的所有分类列表（去重，按字典序）
     *
     * @return 分类名称列表
     */
    List<String> listCategories();
}