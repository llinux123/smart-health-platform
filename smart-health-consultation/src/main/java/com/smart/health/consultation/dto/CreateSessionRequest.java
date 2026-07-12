package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建问诊会话请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建问诊会话请求")
public class CreateSessionRequest {

    @Schema(description = "草稿ID（来自多模态分析流程）")
    private String draftId;

    @Schema(description = "症状描述 / AI分析报告内容")
    private String symptomDraft;

    @Schema(description = "上传文件URL列表（逗号分隔）")
    private String fileUrls;
}
