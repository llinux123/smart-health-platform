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

    @Schema(description = "症状自查草稿完整内容")
    private String symptomDraft;

    @Schema(description = "上传文件URL列表(逗号分隔)")
    private String fileUrls;

    @Schema(description = "AI总结")
    private String aiSummary;

    @Schema(description = "对话轮数")
    private Integer turnCount;

    @Schema(description = "会话状态", allowableValues = {"IN_PROGRESS", "COMPLETED"})
    private String status;

    @Schema(description = "是否置顶")
    private Boolean isPinned;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后对话时间")
    private LocalDateTime lastChatTime;

    @Schema(description = "是否已评分")
    private Boolean hasRating;

    @Schema(description = "指配医生ID")
    private Long assignedDoctorId;

    @Schema(description = "指配医生姓名")
    private String assignedDoctorName;
}
