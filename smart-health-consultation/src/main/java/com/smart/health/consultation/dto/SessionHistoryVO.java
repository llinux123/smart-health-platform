package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话对话历史 VO（单条消息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话对话历史条目")
public class SessionHistoryVO {

    @Schema(description = "角色: user / assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "发送时间")
    private String timestamp;
}
