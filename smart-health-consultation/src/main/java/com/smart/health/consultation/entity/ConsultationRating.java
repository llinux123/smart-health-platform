package com.smart.health.consultation.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问诊评分实体
 */
@Data
public class ConsultationRating {

    /** 评分ID */
    private Long id;

    /** 会话编号 */
    private String sessionSn;

    /** 患者ID */
    private Long patientId;

    /** 评分(1-5星) */
    private Integer rating;

    /** 文字反馈(可选) */
    private String feedback;

    /** 创建时间 */
    private LocalDateTime createTime;
}
