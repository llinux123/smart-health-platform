package com.smart.health.consultation.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问诊对话轮次实体（一轮 = 一次 user + assistant）
 */
@Data
public class ConsultationTurn {

    /** 轮次ID */
    private Long id;

    /** 会话编号 */
    private String sessionSn;

    /** 轮次序号(从1开始) */
    private Integer turnNumber;

    /** 用户消息 */
    private String userMessage;

    /** AI回复 */
    private String assistantMessage;

    /** 引用来源(JSON数组) */
    private String citations;

    /** 发送者类型: PATIENT / AI / DOCTOR */
    private String senderType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
