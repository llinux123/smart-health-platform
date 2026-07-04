package com.smart.health.prescription.dto;

import com.smart.health.prescription.enums.AuditStatus;
import com.smart.health.prescription.enums.PrescriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "处方详情")
public class PrescriptionVO {

    @Schema(description = "处方ID")
    private Long id;

    @Schema(description = "处方编号")
    private String prescriptionSn;

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "医生ID")
    private Long doctorId;

    @Schema(description = "诊断结论")
    private String diagnosis;

    @Schema(description = "PDF存根URL")
    private String pdfUrl;

    @Schema(description = "审核状态")
    private AuditStatus auditStatus;

    @Schema(description = "审核药师ID")
    private Long pharmacistId;

    @Schema(description = "审核意见")
    private String auditComments;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "流转状态")
    private PrescriptionStatus status;

    @Schema(description = "开具时间")
    private LocalDateTime createTime;

    @Schema(description = "药品明细列表")
    private List<PrescriptionItemVO> items;
}
