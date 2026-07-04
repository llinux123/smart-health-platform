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

    /** 上传文件URL列表(逗号分隔) */
    private String fileUrls;

    /** 会话状态，见 {@link com.smart.health.consultation.constant.SessionStatus} */
    private String status;

    /** 是否已删除(回收站) */
    private Boolean isDeleted;

    /** 删除时间(回收站) */
    private LocalDateTime deletedAt;

    /** 是否置顶 */
    private Boolean isPinned;

    /** AI总结 */
    private String aiSummary;

    /** 最后对话时间 */
    private LocalDateTime lastChatTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
