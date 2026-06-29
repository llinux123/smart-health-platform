package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问诊流式响应（SSE）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问诊流式响应")
public class ConsultStreamResponse {

    @Schema(description = "响应内容")
    private String content;
}
