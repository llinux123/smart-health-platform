package com.smart.health.consultation.service;

import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.dto.RatingRequest;
import com.smart.health.consultation.dto.SessionVO;

/**
 * 会话归档 Service — 删除、回收站、评分、定时清理
 */
public interface SessionArchive {

    /**
     * 删除会话（recycle=移入回收站, permanent=物理删除）
     */
    void deleteSession(String sessionSn, Long patientId, String mode);

    /**
     * 评分
     */
    void rateSession(String sessionSn, Long patientId, RatingRequest request);

    /**
     * 回收站列表（分页）
     */
    PageResult<SessionVO> listRecycleBin(Long patientId, int page, int size);

    /**
     * 从回收站恢复
     */
    void restoreSession(String sessionSn, Long patientId);

    /**
     * 回收站中彻底删除
     */
    void permanentDeleteFromRecycleBin(String sessionSn, Long patientId);

    /**
     * 清理回收站中超过30天的记录
     */
    void cleanExpiredRecycleBin();
}
