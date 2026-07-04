package com.smart.health.consultation.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.service.SessionAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * JPA 实现 — 直接查 DB（单体模式）
 *
 * <p>{@link #findAndValidate} 内置自动结束检查（24h 无活动 → COMPLETED），
 * 所有通过该入口获取的会话均已反映最新状态。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaSessionAccessor implements SessionAccessor {

    private static final int AUTO_COMPLETE_HOURS = 24;

    private final ConsultationSessionMapper sessionMapper;

    @Override
    public ConsultationSession findBySessionSn(String sessionSn) {
        return sessionMapper.selectBySessionSn(sessionSn);
    }

    @Override
    public ConsultationSession findBySessionSnIncludeDeleted(String sessionSn) {
        return sessionMapper.selectBySessionSnIncludeDeleted(sessionSn);
    }

    @Override
    public int countByPatientId(Long patientId) {
        return sessionMapper.countByPatientId(patientId);
    }

    @Override
    public ConsultationSession findAndValidate(String sessionSn, Long patientId) {
        ConsultationSession session = SessionAccessor.super.findAndValidate(sessionSn, patientId);
        applyAutoComplete(session);
        return session;
    }

    /**
     * 若会话进行中且超过 24 小时无活动，自动标记为已完成
     */
    private void applyAutoComplete(ConsultationSession session) {
        if (SessionStatus.isInProgress(session.getStatus())
                && session.getLastChatTime() != null
                && session.getLastChatTime().plusHours(AUTO_COMPLETE_HOURS).isBefore(LocalDateTime.now())) {
            sessionMapper.updateStatus(session.getId(), SessionStatus.COMPLETED);
            session.setStatus(SessionStatus.COMPLETED);
            log.info("会话自动结束(24h无活动), sessionSn={}", session.getSessionSn());
        }
    }
}
