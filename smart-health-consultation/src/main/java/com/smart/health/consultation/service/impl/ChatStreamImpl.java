package com.smart.health.consultation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.dto.*;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.entity.ConsultationTurn;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import com.smart.health.consultation.service.ChatStream;
import com.smart.health.consultation.service.RagRetrievalService;
import com.smart.health.consultation.service.SessionAccessor;
import com.smart.health.consultation.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式对话 Service — SSE 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamImpl implements ChatStream {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    private final ConsultationTurnMapper turnMapper;
    private final ConsultationSessionMapper sessionMapper;
    private final SessionAccessor sessionAccessor;
    private final SessionManager sessionManager;
    private final RagRetrievalService ragRetrievalService;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter streamConsult(ConsultStreamRequest request, Long patientId) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        // 查询或自动创建会话（由 Caller 层处理）
        ConsultationSession session;
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            session = sessionAccessor.findAndValidate(request.getSessionId(), patientId);
            if (SessionStatus.isCompleted(session.getStatus())) {
                throw new BusinessException("问诊已结束，无法继续对话");
            }
        } else {
            // 自动创建新会话（恢复旧行为，支持前端首次发消息自动创建）
            String newSessionSn = sessionManager.createSession(patientId, null, request.getMessage(), null);
            session = sessionAccessor.findBySessionSn(newSessionSn);
            if (session == null) {
                throw new BusinessException("会话创建失败");
            }
        }

        // 加载多轮对话历史（从 turn 表）
        List<ConsultationTurn> historyTurns = turnMapper.selectBySessionSnDesc(session.getSessionSn());
        Collections.reverse(historyTurns);

        // RAG 检索
        String ragContext = ragRetrievalService.retrieveAsContext(request.getMessage(), 3);
        List<ConsultStreamResponse.Citation> citations = ragRetrievalService.retrieveCitations(request.getMessage(), 3);

        // 构建 Prompt
        List<Message> messages = buildPromptMessages(ragContext, historyTurns, request.getMessage(), session.getSymptomDraft());

        // 创建 SseEmitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<String> sessionSnRef = new AtomicReference<>(session.getSessionSn());

        Disposable disposable = chatModel.stream(new Prompt(messages))
                .subscribe(
                        chatResponse -> {
                            try {
                                String content = extractContent(chatResponse);
                                if (content != null && !content.isEmpty()) {
                                    fullResponse.append(content);
                                    ConsultStreamResponse resp = ConsultStreamResponse.builder().content(content).build();
                                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(resp)));
                                }
                            } catch (IOException e) {
                                log.error("SSE 发送失败", e);
                            }
                        },
                        error -> {
                            log.error("AI 流式调用异常, sessionSn={}", sessionSnRef.get(), error);
                            try {
                                if (fullResponse.length() > 0) {
                                    saveTurn(session.getSessionSn(), request.getMessage(), fullResponse.toString(), citations);
                                    updateSessionAfterChat(session);
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                } else {
                                    ConsultStreamResponse errResp = ConsultStreamResponse.builder().error("AI 服务暂时不可用，请稍后重试。").build();
                                    emitter.send(SseEmitter.event().name("error").data(objectMapper.writeValueAsString(errResp)));
                                    emitter.complete();
                                }
                            } catch (IOException ignored) {}
                        },
                        () -> {
                            try {
                                if (citations != null && !citations.isEmpty()) {
                                    ConsultStreamResponse citResp = ConsultStreamResponse.builder().content("").citations(citations).build();
                                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(citResp)));
                                }
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();

                                saveTurn(session.getSessionSn(), request.getMessage(), fullResponse.toString(), citations);
                                updateSessionAfterChat(session);
                                log.info("问诊流式响应完成, sessionSn={}, 本轮回答长度={}", sessionSnRef.get(), fullResponse.length());
                            } catch (IOException e) {
                                log.error("SSE 完成发送失败", e);
                            }
                        }
                );

        emitter.onTimeout(() -> {
            log.warn("SSE 超时, sessionSn={}", sessionSnRef.get());
            disposable.dispose();
        });
        emitter.onError(ex -> disposable.dispose());
        emitter.onCompletion(disposable::dispose);

        return emitter;
    }

    @Override
    public TurnVO regenerateLastTurn(String sessionSn, Long patientId, Integer turnNumber) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);

        if (SessionStatus.isCompleted(session.getStatus())) {
            throw new BusinessException("问诊已结束，无法重新生成");
        }

        Integer maxTurn = turnMapper.selectMaxTurnNumber(sessionSn);
        if (maxTurn == null || !maxTurn.equals(turnNumber)) {
            throw new BusinessException("仅可重新生成最后一轮对话");
        }

        ConsultationTurn lastTurn = turnMapper.selectByTurnNumber(sessionSn, turnNumber);
        if (lastTurn == null) {
            throw new BusinessException("对话轮次不存在");
        }

        // 重新调用 AI 生成
        List<ConsultationTurn> historyTurns = turnMapper.selectBySessionSnDesc(sessionSn);
        Collections.reverse(historyTurns);
        historyTurns.remove(historyTurns.size() - 1);

        String ragContext = ragRetrievalService.retrieveAsContext(lastTurn.getUserMessage(), 3);
        List<ConsultStreamResponse.Citation> citations = ragRetrievalService.retrieveCitations(lastTurn.getUserMessage(), 3);
        List<Message> messages = buildPromptMessages(ragContext, historyTurns, lastTurn.getUserMessage(), null);

        // 同步调用 AI（非流式）
        String aiResponse = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
        String citationsJson = serializeCitations(citations);

        turnMapper.updateAssistantMessage(sessionSn, turnNumber, aiResponse, citationsJson);

        ConsultationTurn updatedTurn = turnMapper.selectByTurnNumber(sessionSn, turnNumber);
        return toTurnVO(updatedTurn);
    }

    // ========== 私有方法 ==========

    private void saveTurn(String sessionSn, String userMessage, String assistantMessage, List<ConsultStreamResponse.Citation> citations) {
        Integer maxTurn = turnMapper.selectMaxTurnNumber(sessionSn);
        int nextTurn = (maxTurn == null) ? 1 : maxTurn + 1;

        ConsultationTurn turn = new ConsultationTurn();
        turn.setSessionSn(sessionSn);
        turn.setTurnNumber(nextTurn);
        turn.setUserMessage(userMessage);
        turn.setAssistantMessage(assistantMessage);
        turn.setCitations(serializeCitations(citations));
        turn.setSenderType("AI");
        turnMapper.insert(turn);
    }

    private void updateSessionAfterChat(ConsultationSession session) {
        sessionMapper.updateLastChatTime(session.getId(), LocalDateTime.now());
    }

    private String serializeCitations(List<ConsultStreamResponse.Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            log.warn("序列化 citations 失败: {}", e.getMessage());
            return null;
        }
    }

    private List<Message> buildPromptMessages(String ragContext, List<ConsultationTurn> historyTurns,
                                               String currentMessage, String symptomDraft) {
        List<Message> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        systemContent.append("你是一位经验丰富的全科AI医生助手，基于提供的医学知识和患者的症状信息，");
        systemContent.append("为患者提供专业、准确、易懂的医疗健康咨询服务。\n");
        systemContent.append("重要规则：\n");
        systemContent.append("1. 回答必须基于下方提供的【医学知识库】内容，不要编造不存在的医学事实。\n");
        systemContent.append("2. 在回答正文中使用 [1]、[2] 等编号标注引用来源。\n");
        systemContent.append("3. 始终提醒患者：AI建议仅供参考，具体诊疗请遵医嘱。\n");
        systemContent.append("4. 语言要通俗易懂。\n\n");

        if (symptomDraft != null && !symptomDraft.isBlank()) {
            systemContent.append("【患者症状自查草稿】\n").append(symptomDraft).append("\n\n");
        }
        if (ragContext != null && !ragContext.isBlank()) {
            systemContent.append("【医学知识库参考】\n").append(ragContext).append("\n\n");
        }
        systemContent.append("请基于以上信息回答患者的问题。");
        messages.add(new SystemMessage(systemContent.toString()));

        // 最近 10 轮历史
        int startIdx = Math.max(0, historyTurns.size() - 10);
        for (int i = startIdx; i < historyTurns.size(); i++) {
            ConsultationTurn turn = historyTurns.get(i);
            messages.add(new UserMessage(turn.getUserMessage()));
            messages.add(new AssistantMessage(turn.getAssistantMessage()));
        }

        messages.add(new UserMessage(currentMessage));
        return messages;
    }

    private String extractContent(org.springframework.ai.chat.model.ChatResponse chatResponse) {
        if (chatResponse == null) return null;
        var result = chatResponse.getResult();
        if (result != null && result.getOutput() != null) {
            return result.getOutput().getText();
        }
        return null;
    }

    private TurnVO toTurnVO(ConsultationTurn turn) {
        List<ConsultStreamResponse.Citation> citations = null;
        if (turn.getCitations() != null && !turn.getCitations().isBlank()) {
            try {
                citations = objectMapper.readValue(turn.getCitations(),
                        new TypeReference<List<ConsultStreamResponse.Citation>>() {});
            } catch (JsonProcessingException e) {
                log.warn("解析 citations 失败: {}", e.getMessage());
            }
        }

        return TurnVO.builder()
                .id(turn.getId())
                .turnNumber(turn.getTurnNumber())
                .userMessage(turn.getUserMessage())
                .assistantMessage(turn.getAssistantMessage())
                .citations(citations)
                .senderType(turn.getSenderType())
                .createTime(turn.getCreateTime())
                .build();
    }
}
