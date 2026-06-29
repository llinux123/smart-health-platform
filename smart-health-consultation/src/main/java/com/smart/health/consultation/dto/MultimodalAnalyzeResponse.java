package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多模态图片分析响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "多模态图片分析响应")
public class MultimodalAnalyzeResponse {

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "草稿ID")
    private String draftId;

    @Schema(description = "症状自查草稿内容")
    private String symptomDraft;
}
