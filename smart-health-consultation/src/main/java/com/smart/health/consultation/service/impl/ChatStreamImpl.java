package com.smart.health.consultation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.config.FileUploadConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class ChatStreamImpl implements ChatStream {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /** 连续消息合并窗口（秒）：在此时间内发送的下一条消息将合并到上一轮 */
    private static final long MERGE_WINDOW_SECONDS = 30;

    private final ConsultationTurnMapper turnMapper;
    private final ConsultationSessionMapper sessionMapper;
    private final SessionAccessor sessionAccessor;
    private final SessionManager sessionManager;
    private final RagRetrievalService ragRetrievalService;
    private final OpenAiChatModel chatModel;
    private final ChatModel multimodalChatModel;
    private final FileUploadConfig fileUploadConfig;
    private final ObjectMapper objectMapper;

    public ChatStreamImpl(ConsultationTurnMapper turnMapper,
                          ConsultationSessionMapper sessionMapper,
                          SessionAccessor sessionAccessor,
                          SessionManager sessionManager,
                          RagRetrievalService ragRetrievalService,
                          OpenAiChatModel chatModel,
                          @Qualifier("multimodalChatModel") ChatModel multimodalChatModel,
                          FileUploadConfig fileUploadConfig,
                          ObjectMapper objectMapper) {
        this.turnMapper = turnMapper;
        this.sessionMapper = sessionMapper;
        this.sessionAccessor = sessionAccessor;
        this.sessionManager = sessionManager;
        this.ragRetrievalService = ragRetrievalService;
        this.chatModel = chatModel;
        this.multimodalChatModel = multimodalChatModel;
        this.fileUploadConfig = fileUploadConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter streamConsult(ConsultStreamRequest request, Long patientId) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        // 查询或自动创建会话
        ConsultationSession session;
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            session = sessionAccessor.findAndValidate(request.getSessionId(), patientId);
            if (SessionStatus.isCompleted(session.getStatus())) {
                throw new BusinessException("问诊已结束，无法继续对话");
            }
            if (SessionStatus.PENDING_DOCTOR.equals(session.getStatus())) {
                throw new BusinessException("已转接医生，请等待医生接诊后继续沟通");
            }
            if (SessionStatus.DOCTOR_ACTIVE.equals(session.getStatus())) {
                log.info("医生沟通中, patientId={}, sessionSn={}", patientId, session.getSessionSn());
            }
        } else {
            // 自动创建新会话（恢复旧行为，支持前端首次发消息自动创建）
            String newSessionSn = sessionManager.createSession(patientId, null, request.getMessage(), null);
            session = sessionAccessor.findBySessionSn(newSessionSn);
            if (session == null) {
                throw new BusinessException("会话创建失败");
            }
        }

        // DOCTOR_ACTIVE 状态：患者消息直接存入库，不发 AI 模型
        if (SessionStatus.DOCTOR_ACTIVE.equals(session.getStatus())) {
            savePatientTurn(session.getSessionSn(), request.getMessage());
            updateSessionAfterChat(session);
            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
            try {
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (IOException e) {
                log.error("SSE 完成发送失败", e);
            }
            emitter.onTimeout(() -> {});
            emitter.onError(ex -> {});
            return emitter;
        }

        // 检查是否合并连续消息：若最后一轮在 MERGE_WINDOW 内创建，则合并到该轮
        ConsultationTurn lastTurn = turnMapper.selectLastTurn(session.getSessionSn());
        final boolean shouldMerge = lastTurn != null
                && !"DOCTOR".equals(lastTurn.getSenderType())
                && lastTurn.getCreateTime() != null
                && lastTurn.getCreateTime().isAfter(LocalDateTime.now().minusSeconds(MERGE_WINDOW_SECONDS));
        final String effectiveMessage = shouldMerge
                ? lastTurn.getUserMessage() + "\n" + request.getMessage()
                : request.getMessage();
        final int mergeTurnNumber = shouldMerge ? lastTurn.getTurnNumber() : -1;
        if (shouldMerge) {
            log.info("合并连续消息, sessionSn={}, turnNumber={}", session.getSessionSn(), mergeTurnNumber);
        }

        // 加载多轮对话历史（从 turn 表）
        List<ConsultationTurn> historyTurns = turnMapper.selectBySessionSnDesc(session.getSessionSn());
        Collections.reverse(historyTurns);
        if (shouldMerge && !historyTurns.isEmpty()) {
            // 合并时排除最后一轮（将用 effectiveMessage 替代）
            historyTurns = new ArrayList<>(historyTurns.subList(0, historyTurns.size() - 1));
        }

        // 加载会话中的图片（用于多模态问诊）
        List<Media> sessionImages = loadSessionImages(session.getFileUrls());
        boolean hasImages = sessionImages != null && !sessionImages.isEmpty();

        // RAG 检索
        String ragContext = ragRetrievalService.retrieveAsContext(effectiveMessage, 3);
        List<ConsultStreamResponse.Citation> citations = ragRetrievalService.retrieveCitations(effectiveMessage, 3);

        // 构建 Prompt
        List<Message> messages = buildPromptMessages(ragContext, historyTurns, effectiveMessage,
                session.getSymptomDraft(), sessionImages);

        // 有图片时使用多模态模型
        ChatModel model = hasImages ? multimodalChatModel : chatModel;
        log.info("问诊调用模型: {}, images={}, sessionSn={}", hasImages ? "multimodal" : "text",
                sessionImages != null ? sessionImages.size() : 0, session.getSessionSn());

        // 创建 SseEmitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<String> sessionSnRef = new AtomicReference<>(session.getSessionSn());

        Disposable disposable = model.stream(new Prompt(messages))
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
                                    saveOrUpdateTurn(session.getSessionSn(), effectiveMessage, fullResponse.toString(),
                                            citations, shouldMerge, mergeTurnNumber);
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
                                saveOrUpdateTurn(session.getSessionSn(), effectiveMessage, fullResponse.toString(),
                                        citations, shouldMerge, mergeTurnNumber);
                                updateSessionAfterChat(session);

                                if (citations != null && !citations.isEmpty()) {
                                    ConsultStreamResponse citResp = ConsultStreamResponse.builder().content("").citations(citations).build();
                                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(citResp)));
                                }
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();
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

        List<Media> sessionImages = loadSessionImages(session.getFileUrls());

        String ragContext = ragRetrievalService.retrieveAsContext(lastTurn.getUserMessage(), 3);
        List<ConsultStreamResponse.Citation> citations = ragRetrievalService.retrieveCitations(lastTurn.getUserMessage(), 3);
        List<Message> messages = buildPromptMessages(ragContext, historyTurns, lastTurn.getUserMessage(),
                session.getSymptomDraft(), sessionImages);

        // 同步调用 AI（有图片时用多模态模型）
        ChatModel model = (sessionImages != null && !sessionImages.isEmpty()) ? multimodalChatModel : chatModel;
        String aiResponse = model.call(new Prompt(messages)).getResult().getOutput().getText();
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

    /**
     * 根据是否合并决定插入新轮次或更新已有轮次
     */
    private void saveOrUpdateTurn(String sessionSn, String userMessage, String assistantMessage,
                                   List<ConsultStreamResponse.Citation> citations,
                                   boolean shouldMerge, int mergeTurnNumber) {
        if (shouldMerge) {
            turnMapper.updateUserAndAssistantMessage(sessionSn, mergeTurnNumber,
                    userMessage, assistantMessage, serializeCitations(citations));
            log.info("合并更新对话轮次, sessionSn={}, turnNumber={}", sessionSn, mergeTurnNumber);
        } else {
            saveTurn(sessionSn, userMessage, assistantMessage, citations);
        }
    }

    private void savePatientTurn(String sessionSn, String message) {
        Integer maxTurn = turnMapper.selectMaxTurnNumber(sessionSn);
        int nextTurn = (maxTurn == null) ? 1 : maxTurn + 1;

        ConsultationTurn turn = new ConsultationTurn();
        turn.setSessionSn(sessionSn);
        turn.setTurnNumber(nextTurn);
        turn.setUserMessage(message);
        turn.setAssistantMessage("");
        turn.setSenderType("PATIENT");
        turnMapper.insert(turn);
        log.info("患者消息已保存, sessionSn={}, turnNumber={}", sessionSn, nextTurn);
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
                                               String currentMessage, String symptomDraft, List<Media> sessionImages) {
        List<Message> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        systemContent.append("你是一位经验丰富的全科AI医生助手，基于提供的医学知识和患者的症状信息，");
        systemContent.append("为患者提供专业、准确、易懂的医疗健康咨询服务。\n");
        systemContent.append("重要规则：\n");
        systemContent.append("1. 回答必须基于下方提供的【医学知识库】内容，不要编造不存在的医学事实。\n");
        systemContent.append("2. 在回答正文中使用 [1]、[2] 等编号标注引用来源。\n");
        systemContent.append("3. 始终提醒患者：AI建议仅供参考，具体诊疗请遵医嘱。\n");
        systemContent.append("4. 语言要通俗易懂。\n");
        systemContent.append("5. 作为预问诊医助，当患者描述症状后，请主动追问关键信息：发病时间、症状部位、严重程度、伴随症状、既往史、过敏史。\n");
        systemContent.append("6. 在问诊过程中保持简洁，每次追问 1-2 个问题，避免一次性抛出过多问题给患者。\n\n");

        if (symptomDraft != null && !symptomDraft.isBlank()) {
            systemContent.append("【患者症状自查草稿】\n").append(symptomDraft).append("\n\n");
        }
        if (ragContext != null && !ragContext.isBlank()) {
            systemContent.append("【医学知识库参考】\n").append(ragContext).append("\n\n");
        }
        systemContent.append("请基于以上信息回答患者的问题。");
        messages.add(new SystemMessage(systemContent.toString()));

        // 最近 10 轮历史（仅 AI 轮次，过滤医生回复避免污染提示词）
        int startIdx = Math.max(0, historyTurns.size() - 10);
        for (int i = startIdx; i < historyTurns.size(); i++) {
            ConsultationTurn turn = historyTurns.get(i);
            if ("DOCTOR".equals(turn.getSenderType())) {
                continue;
            }
            messages.add(new UserMessage(turn.getUserMessage()));
            messages.add(new AssistantMessage(turn.getAssistantMessage()));
        }

        // 首轮消息 + 有图片时，通过 UserMessage + Media 发送多模态输入
        boolean hasImages = sessionImages != null && !sessionImages.isEmpty();
        boolean isFirstMessage = historyTurns.isEmpty();
        if (hasImages && isFirstMessage) {
            UserMessage.Builder builder = UserMessage.builder();
            builder.text(currentMessage);
            for (Media media : sessionImages) {
                builder.media(media);
            }
            messages.add(builder.build());
        } else {
            messages.add(new UserMessage(currentMessage));
        }
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

    private List<Media> loadSessionImages(String fileUrls) {
        if (fileUrls == null || fileUrls.isBlank()) {
            return Collections.emptyList();
        }
        List<Media> mediaList = new ArrayList<>();
        for (String url : fileUrls.split(",")) {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path filePath = Paths.get(fileUploadConfig.getUploadPath(), filename);
            if (!Files.exists(filePath)) {
                log.warn("会话图片不存在, url={}, path={}", url, filePath);
                continue;
            }
            String ext = filename.toLowerCase();
            if (!isImageExtension(ext)) {
                continue;
            }
            Resource resource = new FileSystemResource(filePath);
            MimeType mimeType = determineMimeType(filename);
            mediaList.add(new Media(mimeType, resource));
        }
        return mediaList;
    }

    private MimeType determineMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (name.endsWith(".gif")) return MimeTypeUtils.IMAGE_GIF;
        if (name.endsWith(".webp")) return MimeTypeUtils.parseMimeType("image/webp");
        if (name.endsWith(".bmp")) return MimeTypeUtils.parseMimeType("image/bmp");
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private boolean isImageExtension(String filename) {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".gif")
                || filename.endsWith(".webp") || filename.endsWith(".bmp");
    }
}
