package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.TurnVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流式对话 Service — SSE 流式响应、prompt 构建、轮次保存
 */
public interface ChatStream {

    /**
     * SSE 流式问诊
     *
     * @param request   请求参数（包含消息内容和 sessionId）
     * @param patientId 患者ID
     * @return SSE Emitter
     */
    SseEmitter streamConsult(ConsultStreamRequest request, Long patientId);

    /**
     * 重新生成最后一轮 AI 回复（仅支持指定轮次）
     *
     * @param sessionSn 会话编号
     * @param patientId 患者ID
     * @param turnNumber 轮次数
     * @return 重新生成的轮次 VO
     */
    TurnVO regenerateLastTurn(String sessionSn, Long patientId, Integer turnNumber);
}
