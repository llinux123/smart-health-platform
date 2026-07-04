package com.smart.health.consultation.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.impl.JpaSessionAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * SessionAccessor 纯读访问器 — findAndValidate 权限验证测试
 *
 * <p>测试 {@link SessionAccessor#findAndValidate(String, Long)} 的边界情形：
 * <ul>
 *   <li>会话不存在 → BusinessException</li>
 *   <li>患者 ID 不匹配 → BusinessException</li>
 *   <li>正常查找 → 返回会话实体</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionAccessor — 纯读访问器单元测试")
class SessionAccessorTest {

    @Mock
    private ConsultationSessionMapper sessionMapper;

    private SessionAccessor sessionAccessor;

    @BeforeEach
    void setUp() {
        sessionAccessor = new JpaSessionAccessor(sessionMapper);
    }

    private ConsultationSession createSession(Long id, String sessionSn, Long patientId) {
        ConsultationSession session = new ConsultationSession();
        session.setId(id);
        session.setSessionSn(sessionSn);
        session.setPatientId(patientId);
        return session;
    }

    private ConsultationSession createActiveSession(Long id, String sessionSn, Long patientId, LocalDateTime lastChatTime) {
        ConsultationSession session = createSession(id, sessionSn, patientId);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setLastChatTime(lastChatTime);
        return session;
    }

    @Nested
    @DisplayName("findAndValidate — 会话查找与归属验证")
    class FindAndValidate {

        @Test
        @DisplayName("会话不存在时抛出 BusinessException")
        void sessionNotFound_throws() {
            // Given
            when(sessionMapper.selectBySessionSn("nonexistent")).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> sessionAccessor.findAndValidate("nonexistent", 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("问诊会话不存在");
        }

        @Test
        @DisplayName("患者 ID 不匹配时抛出 BusinessException")
        void patientIdMismatch_throws() {
            // Given
            ConsultationSession session = createSession(1L, "session_001", 99L);
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            // When / Then
            assertThatThrownBy(() -> sessionAccessor.findAndValidate("session_001", 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权访问该会话");
        }

        @Test
        @DisplayName("会话存在且归属正确时返回会话实体")
        void validSession_returnsSession() {
            // Given
            ConsultationSession session = createSession(1L, "session_001", 42L);
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            // When
            ConsultationSession result = sessionAccessor.findAndValidate("session_001", 42L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSessionSn()).isEqualTo("session_001");
            assertThat(result.getPatientId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("sessionSn 为 null 时抛出 BusinessException（通过 selectBySessionSn 返回 null 表达）")
        void nullSessionSn_throws() {
            // Given
            when(sessionMapper.selectBySessionSn(null)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> sessionAccessor.findAndValidate(null, 42L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("问诊会话不存在");
        }

        @Test
        @DisplayName("进行中的会话超过 24h 无活动自动结束")
        void autoComplete_inProgressOver24h() {
            // Given
            ConsultationSession session = createActiveSession(1L, "session_001", 42L,
                    LocalDateTime.now().minusHours(25));
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);
            when(sessionMapper.updateStatus(1L, SessionStatus.COMPLETED)).thenReturn(1);

            // When
            ConsultationSession result = sessionAccessor.findAndValidate("session_001", 42L);

            // Then
            assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            verify(sessionMapper).updateStatus(1L, SessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("进行中的会话 24h 内有活动不触发自动结束")
        void noAutoComplete_inProgressWithin24h() {
            // Given
            ConsultationSession session = createActiveSession(1L, "session_001", 42L,
                    LocalDateTime.now().minusHours(12));
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            // When
            ConsultationSession result = sessionAccessor.findAndValidate("session_001", 42L);

            // Then
            assertThat(result.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
            verify(sessionMapper, never()).updateStatus(anyLong(), anyString());
        }

        @Test
        @DisplayName("已完成的会话不触发自动结束")
        void noAutoComplete_alreadyCompleted() {
            // Given
            ConsultationSession session = createSession(1L, "session_001", 42L);
            session.setStatus(SessionStatus.COMPLETED);
            session.setLastChatTime(LocalDateTime.now().minusHours(48));
            when(sessionMapper.selectBySessionSn("session_001")).thenReturn(session);

            // When
            ConsultationSession result = sessionAccessor.findAndValidate("session_001", 42L);

            // Then
            assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            verify(sessionMapper, never()).updateStatus(anyLong(), anyString());
        }
    }
}
