package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问诊流式请求（SSE）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问诊流式请求")
public class ConsultStreamRequest {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "草稿ID")
    private String draftId;

    @Schema(description = "用户消息")
    private String message;
}
