package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话轮次 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话轮次信息")
public class TurnVO {

    @Schema(description = "轮次ID")
    private Long id;

    @Schema(description = "轮次序号")
    private Integer turnNumber;

    @Schema(description = "用户消息")
    private String userMessage;

    @Schema(description = "AI回复")
    private String assistantMessage;

    @Schema(description = "引用来源列表")
    private List<ConsultStreamResponse.Citation> citations;

    @Schema(description = "发送者类型", allowableValues = {"PATIENT", "AI", "DOCTOR"})
    private String senderType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
