package com.smart.health.consultation.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI问诊会话实体
 */
@Data
public class ConsultationSession {

    /** 会话ID */
    private Long id;

    /** 会话编号 */
    private String sessionSn;

    /** 患者ID */
    private Long patientId;

    /** 症状草稿ID */
    private String draftId;

    /** 症状自查草稿内容 */
    private String symptomDraft;

    /** 多轮对话日志(JSON格式) */
    private String chatLog;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
