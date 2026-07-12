package com.smart.health.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 医学知识更新请求（PUT 全量覆盖）
 * 与导入请求字段一致，复用同一组校验
 */
@Data
public class KnowledgeUpdateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容不能超过5000字符")
    private String content;

    @NotBlank(message = "分类不能为空")
    @Size(max = 20, message = "分类不能超过20字符")
    private String category;
}