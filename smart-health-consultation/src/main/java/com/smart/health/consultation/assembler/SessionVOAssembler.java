package com.smart.health.consultation.assembler;

import com.smart.health.consultation.dto.SessionVO;
import com.smart.health.consultation.entity.ConsultationSession;
import com.smart.health.consultation.mapper.ConsultationRatingMapper;
import com.smart.health.consultation.mapper.ConsultationTurnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 会话 VO 装配器 — 统一 entity → VO 转换逻辑
 *
 * <p>消除 {@code SessionManagerImpl} 与 {@code SessionArchiveImpl} 中重复的 toSessionVO 方法。</p>
 */
@Component
@RequiredArgsConstructor
public class SessionVOAssembler {

    private final ConsultationTurnMapper turnMapper;
    private final ConsultationRatingMapper ratingMapper;

    /**
     * 将会话实体转换为列表/详情用 VO
     */
    public SessionVO toVO(ConsultationSession s) {
        int turnCount = turnMapper.countBySessionSn(s.getSessionSn());
        boolean hasRating = ratingMapper.existsBySessionSn(s.getSessionSn());
        String summary = truncateSummary(s.getSymptomDraft(), 100);

        return SessionVO.builder()
                .id(s.getId())
                .sessionSn(s.getSessionSn())
                .symptomDraftSummary(summary)
                .symptomDraft(s.getSymptomDraft())
                .fileUrls(s.getFileUrls())
                .aiSummary(s.getAiSummary())
                .turnCount(turnCount)
                .status(s.getStatus())
                .isPinned(s.getIsPinned())
                .createTime(s.getCreateTime())
                .lastChatTime(s.getLastChatTime())
                .hasRating(hasRating)
                .build();
    }

    private String truncateSummary(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
