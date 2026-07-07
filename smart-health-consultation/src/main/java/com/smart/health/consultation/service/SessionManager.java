package com.smart.health.consultation.service;

import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.dto.*;

/**
 * 会话管理 Service — 会话生命周期（创建、列表、详情、结束、置顶、自动结束）
 */
public interface SessionManager {

    /**
     * 创建新的问诊会话
     *
     * @param patientId     患者ID
     * @param draftId       草稿ID（可选，来自多模态分析流程）
     * @param symptomDraft 症状描述（可选）
     * @param fileUrls     上传文件URL列表（逗号分隔，可选）
     * @return 会话编号（sessionSn）
     */
    String createSession(Long patientId, String draftId, String symptomDraft, String fileUrls);

    /**
     * 分页查询会话列表（支持搜索、筛选）
     */
    PageResult<SessionVO> listSessions(Long patientId, SessionListRequest request);

    /**
     * 获取会话详情
     */
    SessionVO getSessionDetail(String sessionSn, Long patientId);

    /**
     * 分页获取对话轮次
     */
    PageResult<TurnVO> getSessionTurns(String sessionSn, Long patientId, int page, int size);

    /**
     * 确认结束问诊
     */
    void completeSession(String sessionSn, Long patientId);

    /**
     * 置顶/取消置顶切换
     */
    void togglePin(String sessionSn, Long patientId);

    /**
     * 转接真人医生（患者发起）
     * 将 IN_PROGRESS 会话状态变更为 PENDING_DOCTOR
     */
    void handoffSession(String sessionSn, Long patientId, String reason);
}
