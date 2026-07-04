package com.smart.health.consultation.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.assembler.SessionVOAssembler;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.dto.RatingRequest;
import com.smart.health.consultation.dto.SessionVO;
import com.smart.health.consultation.entity.ConsultationRating;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationRatingMapper;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import com.smart.health.consultation.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话归档 Service — 删除/回收站/评分/清理实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionArchiveImpl implements SessionArchive {

    private static final int RECYCLE_BIN_RETENTION_DAYS = 30;

    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationTurnMapper turnMapper;
    private final ConsultationRatingMapper ratingMapper;
    private final SessionAccessor sessionAccessor;
    private final SessionVOAssembler sessionVOAssembler;

    @Override
    @Transactional
    public void deleteSession(String sessionSn, Long patientId, String mode) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);

        if ("recycle".equals(mode)) {
            sessionMapper.softDelete(session.getId());
        } else if ("permanent".equals(mode)) {
            turnMapper.deleteBySessionSn(sessionSn);
            sessionMapper.physicalDelete(session.getId());
        } else {
            throw new BusinessException("无效的删除模式");
        }
    }

    @Override
    @Transactional
    public void rateSession(String sessionSn, Long patientId, RatingRequest request) {
        ConsultationSession session = sessionAccessor.findAndValidate(sessionSn, patientId);

        if (!SessionStatus.isCompleted(session.getStatus())) {
            throw new BusinessException("仅已完成的问诊可以评分");
        }
        if (ratingMapper.existsBySessionSn(sessionSn)) {
            throw new BusinessException("该会话已评分，不可重复评分");
        }

        ConsultationRating rating = new ConsultationRating();
        rating.setSessionSn(sessionSn);
        rating.setPatientId(patientId);
        rating.setRating(request.getRating());
        rating.setFeedback(request.getFeedback());
        ratingMapper.insert(rating);
    }

    @Override
    public PageResult<SessionVO> listRecycleBin(Long patientId, int page, int size) {
        PageHelper.startPage(page, size);
        List<ConsultationSession> sessions = sessionMapper.selectRecycleBinByPatientId(patientId);
        PageInfo<ConsultationSession> pageInfo = new PageInfo<>(sessions);

        List<SessionVO> voList = sessions.stream()
                .map(sessionVOAssembler::toVO)
                .toList();

        return PageResult.of(voList, pageInfo.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void restoreSession(String sessionSn, Long patientId) {
        ConsultationSession session = sessionAccessor.findBySessionSnIncludeDeleted(sessionSn);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作该会话");
        }
        if (!Boolean.TRUE.equals(session.getIsDeleted())) {
            throw new BusinessException("会话未在回收站中");
        }
        sessionMapper.restoreFromRecycleBin(session.getId());
    }

    @Override
    @Transactional
    public void permanentDeleteFromRecycleBin(String sessionSn, Long patientId) {
        ConsultationSession session = sessionAccessor.findBySessionSnIncludeDeleted(sessionSn);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!session.getPatientId().equals(patientId)) {
            throw new BusinessException("无权操作该会话");
        }
        if (!Boolean.TRUE.equals(session.getIsDeleted())) {
            throw new BusinessException("会话未在回收站中");
        }

        turnMapper.deleteBySessionSn(sessionSn);
        sessionMapper.physicalDelete(session.getId());
    }

    @Override
    @Transactional
    public void cleanExpiredRecycleBin() {
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(RECYCLE_BIN_RETENTION_DAYS);
        List<Long> expiredIds = sessionMapper.selectExpiredRecycleBinItems(beforeTime);

        for (Long id : expiredIds) {
            ConsultationSession session = sessionMapper.selectById(id);
            if (session != null) {
                turnMapper.deleteBySessionSn(session.getSessionSn());
                sessionMapper.physicalDelete(id);
            }
        }

        if (!expiredIds.isEmpty()) {
            log.info("回收站自动清理: 清除 {} 条过期记录", expiredIds.size());
        }
    }
}
