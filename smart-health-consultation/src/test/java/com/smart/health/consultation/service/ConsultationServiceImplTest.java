package com.smart.health.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.dto.SessionVO;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.impl.ConsultationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI问诊服务 - Issue #6 业务逻辑测试")
class ConsultationServiceImplTest {

    @Mock
    private ConsultationSessionMapper sessionMapper;

    @Mock
    private RagRetrievalService ragRetrievalService;

    @Mock
    private OpenAiChatClient chatClient;

    private ConsultationServiceImpl consultationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationServiceImpl(
                sessionMapper, ragRetrievalService, chatClient, objectMapper
        );
    }

    @Nested
    @DisplayName("创建问诊会话")
    class CreateSession {

        @Test
        @DisplayName("创建会话返回唯一编号，并持久化到数据库")
        void createSession_validInput_persistsAndReturnsSn() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);

            String sessionSn = consultationService.createSession(42L, "draft_001", "头痛三天");

            assertThat(sessionSn).startsWith("session_");

            ArgumentCaptor<ConsultationSession> captor = ArgumentCaptor.forClass(ConsultationSession.class);
            verify(sessionMapper).insert(captor.capture());

            ConsultationSession saved = captor.getValue();
            assertThat(saved.getPatientId()).isEqualTo(42L);
            assertThat(saved.getDraftId()).isEqualTo("draft_001");
            assertThat(saved.getSymptomDraft()).isEqualTo("头痛三天");
            assertThat(saved.getSessionSn()).isEqualTo(sessionSn);
        }

        @Test
        @DisplayName("创建会话时草稿参数可选")
        void createSession_noDraft_stillCreates() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);

            String sessionSn = consultationService.createSession(42L, null, null);

            assertThat(sessionSn).isNotBlank();
            verify(sessionMapper).insert(argThat(s ->
                    s.getPatientId().equals(42L) &&
                    s.getDraftId() == null &&
                    s.getSymptomDraft() == null
            ));
        }
    }

    @Nested
    @DisplayName("查询会话列表")
    class ListSessions {

        @Test
        @DisplayName("返回患者所有会话，含对话轮数和草稿摘要")
        void listSessions_withSessions_returnsPopulatedList() throws JsonProcessingException {
            String chatLog = objectMapper.writeValueAsString(List.of(
                    java.util.Map.of("role", "user", "content", "头痛", "timestamp", "2026-06-28T10:00:00"),
                    java.util.Map.of("role", "assistant", "content", "建议休息", "timestamp", "2026-06-28T10:00:05"),
                    java.util.Map.of("role", "user", "content", "还痛", "timestamp", "2026-06-28T10:01:00"),
                    java.util.Map.of("role", "assistant", "content", "请就医", "timestamp", "2026-06-28T10:01:05")
            ));

            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn("session_001");
            session.setPatientId(42L);
            session.setSymptomDraft("头痛三天");
            session.setChatLog(chatLog);
            session.setCreateTime(LocalDateTime.of(2026, 6, 28, 10, 0));

            when(sessionMapper.selectByPatientId(42L)).thenReturn(List.of(session));

            List<SessionVO> result = consultationService.listSessions(42L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSessionSn()).isEqualTo("session_001");
            assertThat(result.get(0).getTurnCount()).isEqualTo(2); // 2轮对话
            assertThat(result.get(0).getSymptomDraftSummary()).isEqualTo("头痛三天");
        }

        @Test
        @DisplayName("草稿超过100字时截断显示")
        void listSessions_longDraft_truncatesSummary() {
            String longDraft = "A".repeat(150);
            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn("session_002");
            session.setPatientId(42L);
            session.setSymptomDraft(longDraft);
            session.setChatLog(null);
            session.setCreateTime(LocalDateTime.now());

            when(sessionMapper.selectByPatientId(42L)).thenReturn(List.of(session));

            List<SessionVO> result = consultationService.listSessions(42L);

            assertThat(result.get(0).getSymptomDraftSummary()).hasSize(103); // 100 + "..."
            assertThat(result.get(0).getSymptomDraftSummary()).endsWith("...");
        }

        @Test
        @DisplayName("无会话时返回空列表")
        void listSessions_noSessions_returnsEmptyList() {
            when(sessionMapper.selectByPatientId(42L)).thenReturn(List.of());

            List<SessionVO> result = consultationService.listSessions(42L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("SSE 流式问诊")
    class StreamConsult {

        private ConsultationSession buildSession(Long id, String sn, Long patientId, String chatLog) {
            ConsultationSession session = new ConsultationSession();
            session.setId(id);
            session.setSessionSn(sn);
            session.setPatientId(patientId);
            session.setChatLog(chatLog);
            return session;
        }

        private ChatResponse mockChatResponse(String content) {
            Generation gen = new Generation(content);
            return new ChatResponse(List.of(gen));
        }

        @Test
        @DisplayName("消息为空时抛出 BusinessException")
        void streamConsult_emptyMessage_throws() {
            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("session_001").message("").build();
            assertThatThrownBy(() -> consultationService.streamConsult(request, 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("消息为 null 时抛出 BusinessException")
        void streamConsult_nullMessage_throws() {
            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("session_001").message(null).build();
            assertThatThrownBy(() -> consultationService.streamConsult(request, 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        @DisplayName("sessionId 为 null 时自动创建新会话")
        void streamConsult_nullSessionId_autoCreatesSession() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(ragRetrievalService.retrieveAsContext(anyString(), anyInt())).thenReturn("");
            when(ragRetrievalService.retrieveCitations(anyString(), anyInt())).thenReturn(List.of());
            when(chatClient.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(mockChatResponse("你好")));

            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .message("我头痛").build();
            SseEmitter emitter = consultationService.streamConsult(request, 42L);

            assertThat(emitter).isNotNull();
            ArgumentCaptor<ConsultationSession> captor = ArgumentCaptor.forClass(ConsultationSession.class);
            verify(sessionMapper).insert(captor.capture());
            assertThat(captor.getValue().getSessionSn()).startsWith("session_");
            assertThat(captor.getValue().getPatientId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("sessionId 为空字符串时自动创建新会话")
        void streamConsult_emptySessionId_autoCreatesSession() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(ragRetrievalService.retrieveAsContext(anyString(), anyInt())).thenReturn("");
            when(ragRetrievalService.retrieveCitations(anyString(), anyInt())).thenReturn(List.of());
            when(chatClient.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(Flux.just(mockChatResponse("你好")));

            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("").message("我头痛").build();
            SseEmitter emitter = consultationService.streamConsult(request, 42L);

            assertThat(emitter).isNotNull();
            verify(sessionMapper).insert(any(ConsultationSession.class));
        }

        @Test
        @DisplayName("会话不存在时抛出 BusinessException")
        void streamConsult_sessionNotFound_throws() {
            when(sessionMapper.selectBySessionSn("nonexistent")).thenReturn(null);
            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("nonexistent").message("我头痛").build();

            assertThatThrownBy(() -> consultationService.streamConsult(request, 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("非本人会话时抛出权限异常")
        void streamConsult_wrongPatient_throws() {
            ConsultationSession session = buildSession(1L, "session_001", 99L, null);
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("session_001").message("我头痛").build();

            assertThatThrownBy(() -> consultationService.streamConsult(request, 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权");
        }

        @Test
        @DisplayName("正常流式问诊：加载历史、调用 LLM、保存对话")
        void streamConsult_validRequest_returnsSseEmitterAndSavesChat() throws Exception {
            String existingChatLog = objectMapper.writeValueAsString(List.of(
                    Map.of("role", "user", "content", "之前的提问", "timestamp", "2026-06-28T10:00:00"),
                    Map.of("role", "assistant", "content", "之前的回答", "timestamp", "2026-06-28T10:00:05")
            ));
            ConsultationSession session = buildSession(1L, "session_001", 42L, existingChatLog);
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);
            when(ragRetrievalService.retrieveAsContext(anyString(), anyInt())).thenReturn("知识库上下文");
            when(ragRetrievalService.retrieveCitations(anyString(), anyInt())).thenReturn(List.of());
            when(chatClient.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(
                    Flux.just(mockChatResponse("你好"), mockChatResponse("！"))
            );

            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("session_001").message("我头痛").build();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> savedChatLog = new AtomicReference<>();
            doAnswer(invocation -> {
                savedChatLog.set(invocation.getArgument(1));
                latch.countDown();
                return 1;
            }).when(sessionMapper).updateChatLog(anyLong(), anyString());

            SseEmitter emitter = consultationService.streamConsult(request, 42L);
            assertThat(emitter).isNotNull();

            // 等待异步流式响应完成
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // 验证 chatLog 包含本轮对话
            String chatLog = savedChatLog.get();
            assertThat(chatLog).isNotNull();
            List<Map<String, String>> history = objectMapper.readValue(
                    chatLog, new TypeReference<List<Map<String, String>>>() {});
            // 原有2条 + 本轮user + assistant = 4条
            assertThat(history).hasSize(4);
            assertThat(history.get(2).get("role")).isEqualTo("user");
            assertThat(history.get(2).get("content")).isEqualTo("我头痛");
            assertThat(history.get(3).get("role")).isEqualTo("assistant");
            assertThat(history.get(3).get("content")).isEqualTo("你好！");
        }

        @Test
        @DisplayName("LLM 流式调用异常时返回 SseEmitter 且不抛异常")
        void streamConsult_llmError_returnsEmitterWithoutThrowing() {
            ConsultationSession session = buildSession(1L, "session_001", 42L, null);
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);
            when(ragRetrievalService.retrieveAsContext(anyString(), anyInt())).thenReturn("");
            when(ragRetrievalService.retrieveCitations(anyString(), anyInt())).thenReturn(List.of());
            when(chatClient.stream(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(
                    Flux.error(new RuntimeException("LLM API failure"))
            );

            ConsultStreamRequest request = ConsultStreamRequest.builder()
                    .sessionId("session_001").message("我头痛").build();

            // 应正常返回 SseEmitter，错误在异步流中处理
            SseEmitter emitter = consultationService.streamConsult(request, 42L);
            assertThat(emitter).isNotNull();
        }
    }

    @Nested
    @DisplayName("获取对话历史")
    class GetSessionHistory {

        @Test
        @DisplayName("返回会话完整对话记录")
        void getSessionHistory_validSession_returnsParsedHistory() throws JsonProcessingException {
            String chatLog = objectMapper.writeValueAsString(List.of(
                    java.util.Map.of("role", "user", "content", "我头痛", "timestamp", "2026-06-28T10:00:00"),
                    java.util.Map.of("role", "assistant", "content", "建议就医", "timestamp", "2026-06-28T10:00:05")
            ));

            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn("session_001");
            session.setPatientId(42L);
            session.setChatLog(chatLog);

            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            List<SessionHistoryVO> result = consultationService.getSessionHistory("session_001", 42L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRole()).isEqualTo("user");
            assertThat(result.get(0).getContent()).isEqualTo("我头痛");
            assertThat(result.get(1).getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("会话不存在时抛出BusinessException")
        void getSessionHistory_sessionNotFound_throws() {
            when(sessionMapper.selectBySessionSn("nonexistent")).thenReturn(null);

            assertThatThrownBy(() -> consultationService.getSessionHistory("nonexistent", 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        @DisplayName("非本人会话时抛出权限异常")
        void getSessionHistory_wrongPatient_throws() {
            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn("session_001");
            session.setPatientId(99L); // 其他患者

            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            assertThatThrownBy(() -> consultationService.getSessionHistory("session_001", 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权");
        }

        @Test
        @DisplayName("对话日志为空时返回空历史列表")
        void getSessionHistory_emptyChatLog_returnsEmptyList() {
            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn("session_001");
            session.setPatientId(42L);
            session.setChatLog(null);

            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            List<SessionHistoryVO> result = consultationService.getSessionHistory("session_001", 42L);

            assertThat(result).isEmpty();
        }
    }
}
