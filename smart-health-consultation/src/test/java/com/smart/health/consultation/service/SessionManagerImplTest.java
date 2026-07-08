package com.smart.health.consultation.service;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.sequence.DistributedSequenceGenerator;
import com.smart.health.consultation.assembler.SessionVOAssembler;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.service.impl.SessionManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smart.health.consultation.dto.PreConsultationEmrDTO;
import java.util.Collections;

/**
 * SessionManagerImpl — 会话生命周期管理单元测试
 *
 * <p>测试 {@link SessionManager#createSession} 的 sessionSn 格式规范：
 * <ul>
 *   <li>格式：{@code session_yyyyMMdd_XXXXXX}（基于 Redis INCR 分布式安全）</li>
 *   <li>每次调用生成唯一 sessionSn</li>
 *   <li>可选参数 null 安全</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionManagerImpl — 会话管理单元测试")
class SessionManagerImplTest {

    /** 预期 sessionSn 正则：前缀 + 日期 + 下划线 + 6位序号 */
    private static final Pattern SESSION_SN_PATTERN =
            Pattern.compile("^" + Pattern.quote(CommonConstants.SESSION_SN_PREFIX) + "\\d{8}_\\d{6}$");

    @Mock
    private ConsultationSessionMapper sessionMapper;

    @Mock
    private ConsultationTurnMapper turnMapper;

    @Mock
    private SessionAccessor sessionAccessor;

    @Mock
    private SessionVOAssembler sessionVOAssembler;

    @Mock
    private DistributedSequenceGenerator sequenceGenerator;

    @Mock
    private PreConsultationEmrGenerator emrGenerator;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ObjectMapper objectMapper;

    private SessionManagerImpl sessionManager;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sessionManager = new SessionManagerImpl(sessionMapper, turnMapper,
                sessionAccessor, sessionVOAssembler, sequenceGenerator, emrGenerator, objectMapper,
                transactionTemplate);
        // 让 TransactionTemplate.executeWithoutResult 真正执行回调（lenient 避免 CreateSession 测试报 unnecessary stubbing）
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback =
                    invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Nested
    @DisplayName("completeSession — 结束问诊")
    class CompleteSession {

        @Test
        @DisplayName("结束时生成结构化 EMR 并保存到 ai_summary")
        void completeSession_generatesAndSavesEmr() {
            // Arrange
            ConsultationSession session = buildInProgressSession("session_20260707_000001");
            when(sessionAccessor.findAndValidate("session_20260707_000001", 42L)).thenReturn(session);
            when(turnMapper.selectBySessionSnDesc("session_20260707_000001")).thenReturn(Collections.emptyList());
            PreConsultationEmrDTO emr = new PreConsultationEmrDTO("头痛", "头痛三天", "无", "无", "头颅CT");
            when(emrGenerator.generate(Collections.emptyList(), "头痛")).thenReturn(emr);

            // Act
            sessionManager.completeSession("session_20260707_000001", 42L);

            // Assert
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sessionMapper).updateAiSummary(eq(session.getId()), captor.capture());
            String savedJson = captor.getValue();
            assertThat(savedJson).contains("\"chiefComplaint\":\"头痛\"");
            assertThat(savedJson).contains("\"presentIllness\":\"头痛三天\"");
        }

        private ConsultationSession buildInProgressSession(String sessionSn) {
            ConsultationSession session = new ConsultationSession();
            session.setId(1L);
            session.setSessionSn(sessionSn);
            session.setPatientId(42L);
            session.setSymptomDraft("头痛");
            session.setStatus(SessionStatus.IN_PROGRESS);
            return session;
        }
    }

    @Nested
    @DisplayName("createSession — 会话创建")
    class CreateSession {

        @Test
        @DisplayName("生成的 sessionSn 格式符合 PREFIX + yyyyMMdd + 6位序号")
        void sessionSn_formatIsValid() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

            String sessionSn = sessionManager.createSession(42L, "draft_001", "头痛三天", null);

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertThat(sessionSn)
                    .as("sessionSn 格式应为 session_yyyyMMdd_XXXXXX")
                    .matches(SESSION_SN_PATTERN);
            assertThat(sessionSn).startsWith(CommonConstants.SESSION_SN_PREFIX + today + "_");
            assertThat(sessionSn).endsWith("000001");
        }

        @Test
        @DisplayName("多次调用生成的 sessionSn 不重复")
        void sessionSn_isUniqueAcrossCalls() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(sequenceGenerator.nextFormatted(anyString()))
                    .thenReturn("000001")
                    .thenReturn("000002")
                    .thenReturn("000003")
                    .thenReturn("000004")
                    .thenReturn("000005")
                    .thenReturn("000006")
                    .thenReturn("000007")
                    .thenReturn("000008")
                    .thenReturn("000009")
                    .thenReturn("000010");

            int callCount = 10;
            Set<String> sessionSns = new HashSet<>();
            for (int i = 0; i < callCount; i++) {
                sessionSns.add(sessionManager.createSession(42L, null, null, null));
            }

            assertThat(sessionSns)
                    .as("连续 %d 次调用应生成 %d 个不同 sessionSn", callCount, callCount)
                    .hasSize(callCount);
        }

        @Test
        @DisplayName("生成的 sessionSn 序号递增")
        void sessionSn_sequenceIncreasesMonotonically() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(sequenceGenerator.nextFormatted(anyString()))
                    .thenReturn("000001")
                    .thenReturn("000002");

            String sn1 = sessionManager.createSession(42L, null, null, null);
            String sn2 = sessionManager.createSession(42L, null, null, null);

            assertThat(sn1).endsWith("000001");
            assertThat(sn2).endsWith("000002");
        }

        @Test
        @DisplayName("插入时 patientId、draftId、symptomDraft 正确存入数据库")
        void createSession_persistsCorrectFields() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

            String sessionSn = sessionManager.createSession(42L, "draft_001", "头痛三天", null);

            ArgumentCaptor<ConsultationSession> captor = ArgumentCaptor.forClass(ConsultationSession.class);
            verify(sessionMapper).insert(captor.capture());

            ConsultationSession saved = captor.getValue();
            assertThat(saved.getSessionSn()).isEqualTo(sessionSn);
            assertThat(saved.getPatientId()).isEqualTo(42L);
            assertThat(saved.getDraftId()).isEqualTo("draft_001");
            assertThat(saved.getSymptomDraft()).isEqualTo("头痛三天");
            assertThat(saved.getFileUrls()).isNull();
            assertThat(saved.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
            assertThat(saved.getIsDeleted()).isFalse();
            assertThat(saved.getIsPinned()).isFalse();
        }

        @Test
        @DisplayName("draftId 和 symptomDraft 为 null 时不抛异常")
        void createSession_nullOptionalParams_succeeds() {
            when(sessionMapper.insert(any(ConsultationSession.class))).thenReturn(1);
            when(sequenceGenerator.nextFormatted(anyString())).thenReturn("000001");

            String sessionSn = sessionManager.createSession(42L, null, null, null);
            assertThat(sessionSn).isNotBlank();
            assertThat(sessionSn).matches(SESSION_SN_PATTERN);

            ArgumentCaptor<ConsultationSession> captor = ArgumentCaptor.forClass(ConsultationSession.class);
            verify(sessionMapper).insert(captor.capture());
            assertThat(captor.getValue().getDraftId()).isNull();
            assertThat(captor.getValue().getSymptomDraft()).isNull();
        }
    }
}
