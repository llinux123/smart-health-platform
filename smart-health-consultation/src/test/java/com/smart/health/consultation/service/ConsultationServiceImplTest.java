package com.smart.health.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
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
import org.springframework.ai.openai.OpenAiChatClient;

import java.time.LocalDateTime;
import java.util.List;

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
