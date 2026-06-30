package com.smart.health.consultation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.entity.ConsultationMessage;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationMessageMapper;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.RagRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatClient;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceImplTest {

    @Mock
    private ConsultationSessionMapper sessionMapper;
    @Mock
    private ConsultationMessageMapper messageMapper;
    @Mock
    private RagRetrievalService ragRetrievalService;
    @Mock
    private OpenAiChatClient chatClient;

    private ConsultationServiceImpl consultationService;

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationServiceImpl(
                sessionMapper, messageMapper, ragRetrievalService, chatClient, new ObjectMapper());
    }

    @Test
    @DisplayName("saveChatTurn persists citations on assistant message only")
    void saveChatTurn_persistsAssistantCitations() throws Exception {
        ConsultationSession session = new ConsultationSession();
        session.setId(10L);
        session.setSessionSn("session_test");

        List<ConsultStreamResponse.Citation> citations = List.of(
                ConsultStreamResponse.Citation.builder()
                        .title("指南A")
                        .category("临床指南")
                        .snippet("片段A")
                        .build()
        );

        Method saveChatTurn = ConsultationServiceImpl.class.getDeclaredMethod(
                "saveChatTurn", ConsultationSession.class, List.class, String.class, String.class, List.class);
        saveChatTurn.setAccessible(true);
        saveChatTurn.invoke(consultationService, session, new java.util.ArrayList<>(),
                "用户问题", "AI 回答", citations);

        ArgumentCaptor<ConsultationMessage> messageCaptor = ArgumentCaptor.forClass(ConsultationMessage.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());

        ConsultationMessage userMessage = messageCaptor.getAllValues().get(0);
        ConsultationMessage assistantMessage = messageCaptor.getAllValues().get(1);

        assertThat(userMessage.getRole()).isEqualTo("user");
        assertThat(userMessage.getCitations()).isNull();
        assertThat(assistantMessage.getRole()).isEqualTo("assistant");
        assertThat(assistantMessage.getCitations()).isEqualTo(citations);
        verify(sessionMapper).updateChatLog(eq(10L), any(String.class));
    }

    @Test
    @DisplayName("getSessionHistory returns citations from message mapper")
    void getSessionHistory_returnsPersistedCitations() {
        ConsultationSession session = new ConsultationSession();
        session.setId(1L);
        session.setPatientId(99L);
        session.setSessionSn("session_abc");

        List<ConsultStreamResponse.Citation> citations = List.of(
                ConsultStreamResponse.Citation.builder()
                        .title("指南B")
                        .category("文献")
                        .snippet("片段B")
                        .build()
        );
        SessionHistoryVO assistantHistory = SessionHistoryVO.builder()
                .role("assistant")
                .content("回答")
                .timestamp("2026-06-29T10:00:00")
                .citations(citations)
                .build();

        when(sessionMapper.selectBySessionSn("session_abc")).thenReturn(session);
        when(messageMapper.selectHistoryBySessionId(1L)).thenReturn(List.of(assistantHistory));

        List<SessionHistoryVO> history = consultationService.getSessionHistory("session_abc", 99L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getCitations()).isEqualTo(citations);
    }

    @Test
    @DisplayName("getSessionHistory falls back to chat_log when message table is empty")
    void getSessionHistory_fallsBackToChatLog() throws Exception {
        ConsultationSession session = new ConsultationSession();
        session.setId(1L);
        session.setPatientId(99L);
        session.setSessionSn("session_legacy");
        session.setChatLog("""
                [{"role":"user","content":"old question","timestamp":"2026-06-29T09:00:00"},\
                {"role":"assistant","content":"old answer","timestamp":"2026-06-29T09:00:01"}]
                """);

        when(sessionMapper.selectBySessionSn("session_legacy")).thenReturn(session);
        when(messageMapper.selectHistoryBySessionId(1L)).thenReturn(List.of());

        List<SessionHistoryVO> history = consultationService.getSessionHistory("session_legacy", 99L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo("user");
        assertThat(history.get(0).getTimestamp()).isEqualTo("2026-06-29T09:00:00");
        assertThat(history.get(1).getRole()).isEqualTo("assistant");
        assertThat(history.get(1).getCitations()).isNull();
    }
}
