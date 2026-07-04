package com.smart.health.consultation.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.consultation.entity.ConsultationSession;

/**
 * 会话访问器 — 纯读操作：查找 + 归属验证
 * 
 * <p>面向微服务迁移：单体时直接查 DB，未来替换为远程 HTTP 客户端。</p>
 * Two adapters justify the seam: {@code JpaSessionAccessor} + {@code RemoteSessionAccessor}.
 */
public interface SessionAccessor {

    /**
     * 查找会话并验证归属 + 自动结束检查
     *
     * @param sessionSn 会话编号
     * @param patientId 患者ID
     * @return 已验证归属的会话实体
     * @throws BusinessException 会话不存在或归属不符
     */
    default ConsultationSession findAndValidate(String sessionSn, Long patientId) {
        ConsultationSession session = findBySessionSn(sessionSn);
        if (session == null) {
            throw new BusinessException("问诊会话不存在");
        }
        if (!session.getPatientId().equals(patientId)) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    /**
     * 查找普通会话（不包含已删除）
     */
    ConsultationSession findBySessionSn(String sessionSn);

    /**
     * 查找包含已删除的会话（用于回收站相关操作）
     */
    ConsultationSession findBySessionSnIncludeDeleted(String sessionSn);

    /**
     * 统计患者的问诊会话数量
     *
     * @param patientId 患者ID
     * @return 会话数量
     */
    int countByPatientId(Long patientId);
}
