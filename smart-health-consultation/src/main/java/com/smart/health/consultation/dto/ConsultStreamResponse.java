package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Schema(description = "RAG 引用来源（通常在流式结束前的单独事件中推送）")
    private List<Citation> citations;

    /**
     * RAG 引用来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "RAG 引用来源")
    public static class Citation {

        @Schema(description = "文献标题")
        private String title;

        @Schema(description = "文献类别")
        private String category;

        @Schema(description = "相关片段")
        private String snippet;
    }
}
