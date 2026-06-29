package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问诊流式响应（SSE）
 * 每个 SSE data 事件携带一条该结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问诊流式响应")
public class ConsultStreamResponse {

    @Schema(description = "本次 chunk 的文本内容")
    private String content;

    @Schema(description = "引用来源列表（仅在最后一个事件或单独事件中返回）")
    private List<Citation> citations;

    @Schema(description = "SSE 错误信息（仅在 error 事件中出现）")
    private String error;

    /**
     * 引用来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        @Schema(description = "文档标题")
        private String title;

        @Schema(description = "文档分类")
        private String category;

        @Schema(description = "摘要片段")
        private String snippet;
    }
}
