package com.smart.health.consultation.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.PageResult;
import com.smart.health.common.sequence.DistributedSequenceGenerator;
import com.smart.health.consultation.assembler.SessionVOAssembler;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.dto.*;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.entity.ConsultationTurn;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import com.smart.health.consultation.service.PreConsultationEmrGenerator;
import com.smart.health.consultation.service.SessionAccessor;
import com.smart.health.consultation.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
/**
 * 会话管理 Service — 生命周期管理实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagerImpl implements SessionManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationTurnMapper turnMapper;
    private final SessionAccessor sessionAccessor;
    private final SessionVOAssembler sessionVOAssembler;
    private final DistributedSequenceGenerator sequenceGenerator;
    private final PreConsultationEmrGenerator emrGenerator;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    // ========== SessionManager 职责 ==========

    @Override
    public String createSession(Long patientId, String draftId, String symptomDraft, String fileUrls) {
        String sessionSn = generateSessionSn();
        ConsultationSession session = new ConsultationSession();
        session.setSessionSn(sessionSn);
        session.setPatientId(patientId);
        session.setDraftId(draftId);
        session.setSymptomDraft(symptomDraft);
        session.setFileUrls(fileUrls);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setIsDeleted(false);
        session.setIsPinned(false);
        session.setLastChatTime(LocalDateTime.now());
        sessionMapper.insert(session);
        log.info("创建问诊会话, sessionSn={}, patientId={}", sessionSn, patientId);
        return sessionSn;
    }

    @Override
    public PageResult<SessionVO> listSessions(Long patientId, SessionListRequest request) {
        PageHelper.startPage(request.getPage(), request.getSize());

        String startDate = parseDateStart(request.getStartDate());
        String endDate = parseDateEnd(request.getEndDate());

        List<ConsultationSession> sessions = sessionMapper.selectByPatientIdWithFilter(
                patientId, request.getKeyword(), request.getStatus(),
                request.getIsPinned(), startDate, endDate
        );
        PageInfo<ConsultationSession> pageInfo = new PageInfo<>(sessions);

        List<SessionVO> voList = sessions.stream()
                .map(sessionVOAssembler::toVO)
                .toList();

        return PageResult.of(voList, pageInfo.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionVO getSessionDetail(String sessionSn, Long patientId) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);
        return sessionVOAssembler.toVO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TurnVO> getSessionTurns(String sessionSn, Long patientId, int page, int size) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);

        PageHelper.startPage(page, size);
        List<ConsultationTurn> turns = turnMapper.selectBySessionSnDesc(sessionSn);
        PageInfo<ConsultationTurn> pageInfo = new PageInfo<>(turns);

        List<TurnVO> voList = turns.stream().map(this::toTurnVO).toList();
        return PageResult.of(voList, pageInfo.getTotal(), page, size);
    }

    @Override
    public void completeSession(String sessionSn, Long patientId) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);
        if (SessionStatus.isCompleted(session.getStatus())) {
            throw new BusinessException("问诊已结束");
        }
        String emrJson = generateEmrJson(session);
        saveEmrAndTransition(session, emrJson, SessionStatus.COMPLETED);
        session.setAiSummary(emrJson);
    }

    @Override
    public void togglePin(String sessionSn, Long patientId) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);
        sessionMapper.togglePin(session.getId());
    }

    @Override
    public void handoffSession(String sessionSn, Long patientId, String reason) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);
        if (!SessionStatus.isInProgress(session.getStatus())) {
            throw new BusinessException("当前会话状态不允许转诊，仅进行中的AI对话可转诊");
        }
        String emrJson = generateEmrJson(session);
        saveEmrAndTransition(session, emrJson, SessionStatus.PENDING_DOCTOR);
        session.setAiSummary(emrJson);
        log.info("患者发起转诊, sessionSn={}, patientId={}, reason={}", sessionSn, patientId, reason);
    }

    // ========== 私有辅助方法 ==========

    private String generateSessionSn() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = sequenceGenerator.nextFormatted(CommonConstants.SESSION_SN_COUNTER_PREFIX);
        return CommonConstants.SESSION_SN_PREFIX + today + "_" + seq;
    }

    private TurnVO toTurnVO(ConsultationTurn turn) {
        // Note: citations parsing requires ObjectMapper from ChatStream module
        return TurnVO.builder()
                .id(turn.getId())
                .turnNumber(turn.getTurnNumber())
                .userMessage(turn.getUserMessage())
                .assistantMessage(turn.getAssistantMessage())
                .citations(null)
                .senderType(turn.getSenderType())
                .createTime(turn.getCreateTime())
                .build();
    }

    private String parseDateStart(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        return dateStr;
    }

    private String parseDateEnd(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.plusDays(1).format(DATE_FORMATTER);
        } catch (Exception e) {
            return dateStr;
        }
    }

    /**
     * 调用 LLM 生成结构化 EMR 并序列化为 JSON（在事务外执行）
     */
    private String generateEmrJson(ConsultationSession session) {
        List<ConsultationTurn> turns = turnMapper.selectBySessionSnDesc(session.getSessionSn());
        Collections.reverse(turns);
        var emr = emrGenerator.generate(turns, session.getSymptomDraft());
        try {
            return objectMapper.writeValueAsString(emr);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("序列化结构化 EMR 失败, sessionSn={}: {}", session.getSessionSn(), e.getMessage(), e);
            throw new RuntimeException("结构化 EMR 保存失败", e);
        }
    }

    /**
     * 在单个事务中原子写入 EMR 并更新会话状态
     */
    private void saveEmrAndTransition(ConsultationSession session, String emrJson, String newStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            sessionMapper.updateAiSummary(session.getId(), emrJson);
            sessionMapper.updateStatus(session.getId(), newStatus);
        });
    }
}
