package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 医生端问诊会话列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "医生端问诊会话概要")
public class DoctorConsultSessionVO {

    @Schema(description = "会话编号")
    private String sessionSn;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "患者性别 (0:未知 1:男 2:女)")
    private Integer patientGender;

    @Schema(description = "患者年龄")
    private Integer patientAge;

    @Schema(description = "症状摘要（前80字）")
    private String symptomSummary;

    @Schema(description = "上传附件数量")
    private Integer fileCount;

    @Schema(description = "对话轮数")
    private Integer turnCount;

    @Schema(description = "会话状态", allowableValues = {"PENDING_DOCTOR", "DOCTOR_ACTIVE"})
    private String status;

    @Schema(description = "最后对话时间")
    private LocalDateTime lastChatTime;
}
