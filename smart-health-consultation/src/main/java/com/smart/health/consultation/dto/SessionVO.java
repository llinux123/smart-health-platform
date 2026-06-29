package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问诊会话列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问诊会话概要信息")
public class SessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话编号")
    private String sessionSn;

    @Schema(description = "症状自查草稿摘要（前100字）")
    private String symptomDraftSummary;

    @Schema(description = "对话轮数")
    private Integer turnCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
