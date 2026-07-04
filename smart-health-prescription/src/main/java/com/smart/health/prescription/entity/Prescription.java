package com.smart.health.prescription.entity;

import com.smart.health.prescription.enums.AuditStatus;
import com.smart.health.prescription.enums.PrescriptionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子处方实体
 * 对应表: t_prescription
 */
@Data
public class Prescription {

    /** 处方ID */
    private Long id;

    /** 处方全国唯一编码 */
    private String prescriptionSn;

    /** 患者ID */
    private Long patientId;

    /** 开具医生ID */
    private Long doctorId;

    /** 临床诊断结论 */
    private String diagnosis;

    /** 电子处方PDF存根路径 */
    private String pdfUrl;

    /** 药师审核状态 */
    private AuditStatus auditStatus;

    /** 审核药师ID */
    private Long pharmacistId;

    /** 审核意见 */
    private String auditComments;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 流转状态 */
    private PrescriptionStatus status;

    /** 开具时间 */
    private LocalDateTime createTime;
}
