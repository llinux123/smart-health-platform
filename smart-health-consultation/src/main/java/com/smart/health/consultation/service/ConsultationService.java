package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.ConsultStreamRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 问诊服务接口
 * 提供基于 RAG + 多轮会话的 SSE 流式问诊能力
 */
public interface ConsultationService {

    /**
     * SSE 流式问诊
     * 基于 RAG 检索增强 + 多轮对话历史，以 SSE 流式方式返回 AI 回答
     *
     * @param request   问诊请求（sessionId, message）
     * @param patientId 当前登录患者ID
     * @return SseEmitter 用于 SSE 流式推送
     */
    SseEmitter streamConsult(ConsultStreamRequest request, Long patientId);
}
