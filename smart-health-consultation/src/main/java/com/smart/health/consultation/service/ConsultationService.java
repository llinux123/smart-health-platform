package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.dto.SessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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

    /**
     * 创建新的问诊会话
     *
     * @param patientId    患者ID
     * @param draftId      症状草稿ID（可选）
     * @param symptomDraft 症状自查草稿内容（可选）
     * @return 创建的会话编号 sessionSn
     */
    String createSession(Long patientId, String draftId, String symptomDraft);

    /**
     * 查询患者的所有问诊会话列表
     *
     * @param patientId 患者ID
     * @return 会话列表
     */
    List<SessionVO> listSessions(Long patientId);

    /**
     * 获取会话的完整对话历史
     *
     * @param sessionSn 会话编号
     * @param patientId 患者ID
     * @return 对话历史列表
     */
    List<SessionHistoryVO> getSessionHistory(String sessionSn, Long patientId);
}
