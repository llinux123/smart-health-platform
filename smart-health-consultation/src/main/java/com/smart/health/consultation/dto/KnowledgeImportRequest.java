package com.smart.health.consultation.dto;

import lombok.Data;

/**
 * 医学知识导入请求
 */
@Data
public class KnowledgeImportRequest {

    /** 文档标题 */
    private String title;

    /** 文档内容 */
    private String content;

    /** 分类（科室） */
    private String category;
}
