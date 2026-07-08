package com.smart.health.consultation.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.PageResult;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.consultation.constant.SessionStatus;
import com.smart.health.consultation.dto.*;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.entity.ConsultationTurn;
import com.smart.health.consultation.mapper.ConsultationSessionMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import com.smart.health.consultation.mapper.PatientMapper;
import com.smart.health.consultation.service.DoctorConsultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 医生端问诊 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorConsultServiceImpl implements DoctorConsultService {

    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationTurnMapper turnMapper;
    private final PatientMapper patientMapper;

    @Override
    public PageResult<DoctorConsultSessionVO> listPending(DoctorConsultListRequest request) {
        Long doctorId = SecurityUtils.getCurrentDoctorId();
        PageHelper.startPage(request.getPage(), request.getSize());

        List<ConsultationSession> sessions = sessionMapper.selectPendingForDoctor(doctorId, request.getKeyword());
        PageInfo<ConsultationSession> pageInfo = new PageInfo<>(sessions);

        List<DoctorConsultSessionVO> vos = sessions.stream()
                .map(this::toDoctorSessionVO)
                .collect(Collectors.toList());

        return PageResult.of(vos, pageInfo.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorConsultDetailVO getDetail(String sessionSn) {
        ConsultationSession session = findSession(sessionSn);
        List<ConsultationTurn> turns = turnMapper.selectBySessionSnDesc(sessionSn);
        Collections.reverse(turns);

        String patientName = patientMapper.selectNameById(session.getPatientId());
        Integer gender = patientMapper.selectGenderById(session.getPatientId());
        Integer age = calculateAge(patientMapper.selectBirthdayById(session.getPatientId()));

        List<String> fileUrls = parseFileUrls(session.getFileUrls());

        List<TurnVO> turnVOs = turns.stream().map(this::toTurnVO).collect(Collectors.toList());

        return DoctorConsultDetailVO.builder()
                .sessionSn(session.getSessionSn())
                .patientName(patientName != null ? patientName : "未知")
                .patientGender(gender != null ? gender : 0)
                .patientAge(age)
                .symptomDraft(session.getSymptomDraft())
                .aiSummary(session.getAiSummary())
                .fileUrls(fileUrls)
                .status(session.getStatus())
                .turns(turnVOs)
                .createTime(session.getCreateTime())
                .lastChatTime(session.getLastChatTime())
                .build();
    }

    @Override
    @Transactional
    public DoctorConsultReplyVO reply(String sessionSn, DoctorConsultReplyRequest request) {
        ConsultationSession session = findSession(sessionSn);

        if (!SessionStatus.PENDING_DOCTOR.equals(session.getStatus())
                && !SessionStatus.DOCTOR_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException("当前状态不允许回复，仅待接诊或沟通中可回复");
        }

        Integer maxTurn = turnMapper.selectMaxTurnNumber(sessionSn);
        int nextTurn = (maxTurn == null) ? 1 : maxTurn + 1;

        ConsultationTurn turn = new ConsultationTurn();
        turn.setSessionSn(sessionSn);
        turn.setTurnNumber(nextTurn);
        turn.setUserMessage("");
        turn.setAssistantMessage(request.getMessage());
        turn.setSenderType("DOCTOR");
        turn.setCreateTime(LocalDateTime.now());
        turnMapper.insert(turn);

        String newStatus = session.getStatus();
        if (SessionStatus.PENDING_DOCTOR.equals(session.getStatus())) {
            Long doctorId = SecurityUtils.getCurrentDoctorId();
            sessionMapper.updateStatusAndDoctor(session.getId(), SessionStatus.DOCTOR_ACTIVE, doctorId);
            newStatus = SessionStatus.DOCTOR_ACTIVE;
        }

        if ("RESOLVE".equals(request.getAction())) {
            sessionMapper.updateStatus(session.getId(), SessionStatus.COMPLETED);
            newStatus = SessionStatus.COMPLETED;
        }

        sessionMapper.updateLastChatTime(session.getId(), LocalDateTime.now());

        log.info("医生回复问诊, sessionSn={}, turnNumber={}, action={}", sessionSn, nextTurn, request.getAction());

        return DoctorConsultReplyVO.builder()
                .turnNumber(nextTurn)
                .sessionStatus(newStatus)
                .build();
    }

    @Override
    @Transactional
    public void resolve(String sessionSn) {
        ConsultationSession session = findSession(sessionSn);
        if (SessionStatus.isCompleted(session.getStatus())) {
            throw new BusinessException("会话已结束");
        }
        sessionMapper.updateStatus(session.getId(), SessionStatus.COMPLETED);
        log.info("医生标记问诊已解决, sessionSn={}", sessionSn);
    }

    // ========== 私有方法 ==========

    private ConsultationSession findSession(String sessionSn) {
        ConsultationSession session = sessionMapper.selectBySessionSnIncludeDeleted(sessionSn);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    private DoctorConsultSessionVO toDoctorSessionVO(ConsultationSession s) {
        String patientName = patientMapper.selectNameById(s.getPatientId());
        Integer gender = patientMapper.selectGenderById(s.getPatientId());
        Integer age = calculateAge(patientMapper.selectBirthdayById(s.getPatientId()));

        int turnCount = s.getId() != null ? turnMapper.countBySessionSn(s.getSessionSn()) : 0;
        int fileCount = countFiles(s.getFileUrls());
        String summary = truncateString(s.getSymptomDraft(), 80);

        return DoctorConsultSessionVO.builder()
                .sessionSn(s.getSessionSn())
                .patientName(patientName != null ? patientName : "未知")
                .patientGender(gender != null ? gender : 0)
                .patientAge(age)
                .symptomSummary(summary)
                .fileCount(fileCount)
                .turnCount(turnCount)
                .status(s.getStatus())
                .lastChatTime(s.getLastChatTime())
                .build();
    }

    private TurnVO toTurnVO(ConsultationTurn turn) {
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

    private Integer calculateAge(java.sql.Date birthday) {
        if (birthday == null) return null;
        return Period.between(birthday.toLocalDate(), LocalDate.now()).getYears();
    }

    private int countFiles(String fileUrls) {
        if (fileUrls == null || fileUrls.isBlank()) return 0;
        return (int) Arrays.stream(fileUrls.split(",")).filter(s -> !s.isBlank()).count();
    }

    private List<String> parseFileUrls(String fileUrls) {
        if (fileUrls == null || fileUrls.isBlank()) return Collections.emptyList();
        return Arrays.stream(fileUrls.split(","))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String truncateString(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
