package com.smart.health.prescription.service;

import com.smart.health.prescription.dto.InventoryVO;
import com.smart.health.prescription.dto.PrescriptionAuditRequest;
import com.smart.health.prescription.dto.PrescriptionIssueRequest;
import com.smart.health.prescription.dto.PrescriptionVO;

import java.util.List;

/**
 * 处方服务接口
 */
public interface PrescriptionService {

    /**
     * 开具处方（含库存扣减，事务保证原子性）
     *
     * @param request  开具处方请求
     * @param doctorId 开具医生ID
     * @return 处方详情
     */
    PrescriptionVO issuePrescription(PrescriptionIssueRequest request, Long doctorId);

    /**
     * 查询患者处方列表
     *
     * @param patientId 患者ID
     * @return 处方列表（不含药品明细）
     */
    List<PrescriptionVO> listByPatient(Long patientId);

    /**
     * 查询处方详情（含药品明细）
     *
     * @param prescriptionId 处方ID
     * @param patientId      患者ID（用于权限校验，可为null表示不校验）
     * @return 处方详情
     */
    PrescriptionVO getDetail(Long prescriptionId, Long patientId);

    /**
     * 查询药房库存列表
     *
     * @param pharmacyId 药房ID
     * @return 库存列表
     */
    List<InventoryVO> listInventory(Long pharmacyId);

    /**
     * 审核处方（通过或驳回，驳回时恢复库存）
     *
     * @param prescriptionId 处方ID
     * @param request        审核请求（action + comments）
     * @param pharmacistId   审核药师ID
     * @return 处方详情
     */
    PrescriptionVO auditPrescription(Long prescriptionId, PrescriptionAuditRequest request, Long pharmacistId);

    /**
     * 查询待审核处方列表
     *
     * @return 待审核处方列表
     */
    List<PrescriptionVO> listPendingAudit();
}
