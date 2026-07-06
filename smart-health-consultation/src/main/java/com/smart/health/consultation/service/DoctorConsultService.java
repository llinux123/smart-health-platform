package com.smart.health.consultation.service;

import com.smart.health.common.result.PageResult;
import com.smart.health.consultation.dto.*;

/**
 * 医生端问诊 Service — 接诊、查看、回复
 */
public interface DoctorConsultService {

    /**
     * 分页查询待接诊/沟通中的会话
     */
    PageResult<DoctorConsultSessionVO> listPending(DoctorConsultListRequest request);

    /**
     * 查看会话详情（含患者信息、AI分析、对话历史）
     */
    DoctorConsultDetailVO getDetail(String sessionSn);

    /**
     * 回复患者
     */
    DoctorConsultReplyVO reply(String sessionSn, DoctorConsultReplyRequest request);

    /**
     * 标记已解决
     */
    void resolve(String sessionSn);
}
