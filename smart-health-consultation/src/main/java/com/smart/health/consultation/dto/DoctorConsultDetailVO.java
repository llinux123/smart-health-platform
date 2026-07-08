package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 医生端问诊会话详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "医生端问诊会话详情")
public class DoctorConsultDetailVO {

    @Schema(description = "会话编号")
    private String sessionSn;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "患者性别 (0:未知 1:男 2:女)")
    private Integer patientGender;

    @Schema(description = "患者年龄")
    private Integer patientAge;

    @Schema(description = "症状自查草稿（AI分析报告）")
    private String symptomDraft;

    @Schema(description = "结构化预问诊电子病历（JSON 字符串）")
    private String aiSummary;

    @Schema(description = "上传文件URL列表")
    private List<String> fileUrls;

    @Schema(description = "会话状态")
    private String status;

    @Schema(description = "对话轮次列表")
    private List<TurnVO> turns;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后对话时间")
    private LocalDateTime lastChatTime;
}
