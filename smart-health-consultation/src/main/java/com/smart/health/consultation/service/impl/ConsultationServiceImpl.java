package com.smart.health.consultation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.entity.ConsultationMessage;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationMessageMapper;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.ConsultationService;
import com.smart.health.consultation.service.RagRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 问诊服务实现
 * 核心链路：RAG 检索 → 组装 Prompt（系统角色 + 知识上下文 + 多轮历史 + 当前问题）→ 流式调用 LLM → SSE 推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationMessageMapper messageMapper;
    private final RagRetrievalService ragRetrievalService;
    private final OpenAiChatClient chatClient;
    private final ObjectMapper objectMapper;

    /** SSE 超时时间：5 分钟 */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    @Override
    public SseEmitter streamConsult(ConsultStreamRequest request, Long patientId) {
        // 1. 参数校验
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BusinessException("会话ID不能为空");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        // 2. 查询或创建会话
        ConsultationSession session = sessionMapper.selectBySessionSn(request.getSessionId());
        if (session == null) {
            throw new BusinessException("问诊会话不存在: " + request.getSessionId());
        }

        // 3. 加载多轮对话历史
        List<Map<String, String>> chatHistory = parseChatLog(session.getChatLog());

        // 4. RAG 检索：从 ES 知识库检索相关医学文献
        String ragContext = ragRetrievalService.retrieveAsContext(request.getMessage(), 3);
        List<ConsultStreamResponse.Citation> citations =
                ragRetrievalService.retrieveCitations(request.getMessage(), 3);

        // 5. 构建 Prompt
        List<Message> messages = buildPromptMessages(ragContext, chatHistory, request.getMessage(),
                session.getSymptomDraft());

        // 6. 创建 SseEmitter 并发起流式调用
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        StringBuilder fullResponse = new StringBuilder();

        Disposable disposable = chatClient.stream(new Prompt(messages))
                .subscribe(
                        chatResponse -> {
                            try {
                                String content = extractContent(chatResponse);
                                if (content != null && !content.isEmpty()) {
                                    fullResponse.append(content);
                                    ConsultStreamResponse resp = ConsultStreamResponse.builder()
                                            .content(content)
                                            .build();
                                    emitter.send(SseEmitter.event()
                                            .data(objectMapper.writeValueAsString(resp)));
                                }
                            } catch (IOException e) {
                                log.error("SSE 发送失败", e);
                            }
                        },
                        error -> {
                            log.error("AI 流式调用异常", error);
                            try {
                                String fallback = "抱歉，AI 服务暂时不可用，请稍后重试。";
                                ConsultStreamResponse resp = ConsultStreamResponse.builder()
                                        .content(fallback)
                                        .build();
                                emitter.send(SseEmitter.event()
                                        .data(objectMapper.writeValueAsString(resp)));
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ignored) {
                            }
                        },
                        () -> {
                            try {
                                if (!citations.isEmpty()) {
                                    ConsultStreamResponse citationResp = ConsultStreamResponse.builder()
                                            .citations(citations)
                                            .build();
                                    emitter.send(SseEmitter.event()
                                            .data(objectMapper.writeValueAsString(citationResp)));
                                }

                                // 发送结束标记
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();

                                // 保存对话记录到会话
                                saveChatTurn(session, chatHistory, request.getMessage(),
                                        fullResponse.toString(), citations);
                                log.info("问诊流式响应完成, sessionSn={}, 本轮回答长度={}",
                                        session.getSessionSn(), fullResponse.length());
                            } catch (IOException e) {
                                log.error("SSE 完成发送失败", e);
                            }
                        }
                );

        // 超时/错误/完成时释放订阅
        emitter.onTimeout(() -> {
            log.warn("SSE 超时, sessionSn={}", session.getSessionSn());
            disposable.dispose();
        });
        emitter.onError(ex -> disposable.dispose());
        emitter.onCompletion(disposable::dispose);

        return emitter;
    }

    @Override
    public List<SessionHistoryVO> getSessionHistory(String sessionSn, Long patientId) {
        if (sessionSn == null || sessionSn.isBlank()) {
            throw new BusinessException("会话ID不能为空");
        }

        ConsultationSession session = sessionMapper.selectBySessionSn(sessionSn);
        if (session == null) {
            throw new BusinessException("问诊会话不存在: " + sessionSn);
        }
        if (patientId != null && patientId > 0 && !patientId.equals(session.getPatientId())) {
            throw new BusinessException("无权访问该问诊会话");
        }

        return loadSessionHistory(session);
    }

    private List<SessionHistoryVO> loadSessionHistory(ConsultationSession session) {
        List<SessionHistoryVO> history = messageMapper.selectHistoryBySessionId(session.getId());
        if (!history.isEmpty()) {
            return history;
        }
        return parseChatLogHistory(session.getChatLog());
    }

    /**
     * Fallback for sessions created before t_consultation_message existed.
     * Returns messages without citations.
     */
    private List<SessionHistoryVO> parseChatLogHistory(String chatLog) {
        List<Map<String, String>> turns = parseChatLog(chatLog);
        List<SessionHistoryVO> history = new ArrayList<>();
        for (Map<String, String> turn : turns) {
            history.add(SessionHistoryVO.builder()
                    .role(turn.get("role"))
                    .content(turn.get("content"))
                    .timestamp(turn.get("timestamp"))
                    .build());
        }
        return history;
    }

    /**
     * 构建 Prompt 消息列表
     * 结构：SystemMessage(角色 + RAG上下文 + 症状草稿) → 多轮历史 → 当前用户消息
     */
    private List<Message> buildPromptMessages(String ragContext, List<Map<String, String>> chatHistory,
                                               String currentMessage, String symptomDraft) {
        List<Message> messages = new ArrayList<>();

        // System Message: 角色设定 + RAG 知识上下文 + 症状草稿
        StringBuilder systemContent = new StringBuilder();
        systemContent.append("你是一位经验丰富的全科AI医生助手，基于提供的医学知识和患者的症状信息，");
        systemContent.append("为患者提供专业、准确、易懂的医疗健康咨询服务。\n");
        systemContent.append("重要规则：\n");
        systemContent.append("1. 回答必须基于下方提供的【医学知识库】内容，不要编造不存在的医学事实。\n");
        systemContent.append("2. 在回答末尾标注引用的知识来源（如：参考《xxx诊疗指南》）。\n");
        systemContent.append("3. 始终提醒患者：AI建议仅供参考，具体诊疗请遵医嘱。\n");
        systemContent.append("4. 语言要通俗易懂，避免过多专业术语。\n\n");

        if (symptomDraft != null && !symptomDraft.isBlank()) {
            systemContent.append("【患者症状自查草稿】\n").append(symptomDraft).append("\n\n");
        }

        if (ragContext != null && !ragContext.isBlank()) {
            systemContent.append("【医学知识库参考】\n").append(ragContext).append("\n\n");
        }

        systemContent.append("请基于以上信息回答患者的问题。");
        messages.add(new SystemMessage(systemContent.toString()));

        // 多轮对话历史（最近 10 轮）
        int historySize = chatHistory.size();
        int startIdx = Math.max(0, historySize - 20); // 保留最近 20 条消息（10 轮对话）
        for (int i = startIdx; i < historySize; i++) {
            Map<String, String> turn = chatHistory.get(i);
            String role = turn.get("role");
            String content = turn.get("content");
            if ("user".equals(role)) {
                messages.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            }
        }

        // 当前用户消息
        messages.add(new UserMessage(currentMessage));

        return messages;
    }

    /**
     * 从 ChatResponse 中提取文本内容
     */
    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null) return null;
        // Spring AI 0.8.1: 尝试 getResult() 获取第一个 Generation
        var result = chatResponse.getResult();
        if (result != null && result.getOutput() != null) {
            return result.getOutput().getContent();
        }
        return null;
    }

    /**
     * 解析 chatLog JSON 为对话历史列表
     */
    private List<Map<String, String>> parseChatLog(String chatLog) {
        if (chatLog == null || chatLog.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(chatLog, new TypeReference<List<Map<String, String>>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析 chatLog 失败, 将重置为空: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 保存一轮对话（用户消息 + AI 回复）到会话的 chatLog
     */
    private void saveChatTurn(ConsultationSession session, List<Map<String, String>> existingHistory,
                               String userMessage, String assistantResponse,
                               List<ConsultStreamResponse.Citation> citations) {
        // 追加本轮对话
        Map<String, String> userTurn = new LinkedHashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        userTurn.put("timestamp", LocalDateTime.now().toString());
        existingHistory.add(userTurn);

        Map<String, String> assistantTurn = new LinkedHashMap<>();
        assistantTurn.put("role", "assistant");
        assistantTurn.put("content", assistantResponse);
        assistantTurn.put("timestamp", LocalDateTime.now().toString());
        existingHistory.add(assistantTurn);

        try {
            String updatedChatLog = objectMapper.writeValueAsString(existingHistory);
            sessionMapper.updateChatLog(session.getId(), updatedChatLog);
        } catch (JsonProcessingException e) {
            log.error("序列化 chatLog 失败", e);
        }

        ConsultationMessage userMsg = new ConsultationMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messageMapper.insert(userMsg);

        ConsultationMessage assistantMsg = new ConsultationMessage();
        assistantMsg.setSessionId(session.getId());
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantResponse);
        assistantMsg.setCitations(citations == null || citations.isEmpty() ? null : citations);
        messageMapper.insert(assistantMsg);

        log.debug("对话记录已保存, sessionSn={}, 总轮数={}", session.getSessionSn(), existingHistory.size() / 2);
    }
}
