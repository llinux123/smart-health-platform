package com.smart.health.prescription.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.prescription.dto.InventoryVO;
import com.smart.health.prescription.dto.PrescriptionAuditRequest;
import com.smart.health.prescription.dto.PrescriptionIssueRequest;
import com.smart.health.prescription.dto.PrescriptionItemVO;
import com.smart.health.prescription.dto.PrescriptionVO;
import com.smart.health.prescription.entity.PharmacyInventory;
import com.smart.health.prescription.entity.Prescription;
import com.smart.health.prescription.entity.PrescriptionItem;
import com.smart.health.prescription.mapper.PharmacyInventoryMapper;
import com.smart.health.prescription.mapper.PrescriptionItemMapper;
import com.smart.health.prescription.mapper.PrescriptionMapper;
import com.smart.health.prescription.service.PdfGenerationService;
import com.smart.health.prescription.service.PrescriptionService;
import com.smart.health.prescription.util.PrescriptionCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处方服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final PharmacyInventoryMapper pharmacyInventoryMapper;
    private final PdfGenerationService pdfGenerationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO issuePrescription(PrescriptionIssueRequest request, Long doctorId) {
        // 1. 校验所有药品库存是否充足
        validateInventory(request.getMedicines());

        // 2. 生成处方编号
        String prescriptionSn = PrescriptionCodeGenerator.generate();

        // 3. 创建处方主记录
        Prescription prescription = new Prescription();
        prescription.setPrescriptionSn(prescriptionSn);
        prescription.setPatientId(request.getPatientId());
        prescription.setDoctorId(doctorId);
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setAuditStatus(0); // 待审核
        prescription.setStatus(0);      // 未配药
        prescriptionMapper.insert(prescription);

        // 4. 扣减库存 + 创建处方明细
        List<PrescriptionItem> items = new ArrayList<>();
        for (PrescriptionIssueRequest.MedicineItem medicine : request.getMedicines()) {
            // 原子扣减库存（条件更新，库存不足时返回0）
            int affected = pharmacyInventoryMapper.deductStock(
                    medicine.getPharmacyId(),
                    medicine.getMedicineId(),
                    medicine.getQuantity()
            );
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                        "药品「" + medicine.getMedicineName() + "」库存不足");
            }

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescriptionId(prescription.getId());
            item.setMedicineId(medicine.getMedicineId());
            item.setMedicineName(medicine.getMedicineName());
            item.setPharmacyId(medicine.getPharmacyId());
            item.setQuantity(medicine.getQuantity());
            item.setUnit(medicine.getUnit());
            items.add(item);
        }

        // 5. 批量插入处方明细
        if (!items.isEmpty()) {
            prescriptionItemMapper.batchInsert(items);
        }

        // 6. 生成PDF存根
        String pdfUrl = pdfGenerationService.generatePrescriptionPdf(
                prescription.getId(), prescriptionSn,
                request.getPatientId(), doctorId, request.getDiagnosis()
        );
        if (pdfUrl != null) {
            prescriptionMapper.updatePdfUrl(prescription.getId(), pdfUrl);
        }

        log.info("处方开具成功: sn={}, patientId={}, doctorId={}, items={}",
                prescriptionSn, request.getPatientId(), doctorId, items.size());

        // 7. 返回处方详情
        return buildPrescriptionVO(prescription, items);
    }

    @Override
    public List<PrescriptionVO> listByPatient(Long patientId) {
        List<Prescription> prescriptions = prescriptionMapper.selectByPatientId(patientId);
        return prescriptions.stream()
                .map(p -> buildPrescriptionVO(p, null))
                .toList();
    }

    @Override
    public PrescriptionVO getDetail(Long prescriptionId, Long patientId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException(ResultCode.PRESCRIPTION_NOT_FOUND);
        }
        if (patientId != null && !prescription.getPatientId().equals(patientId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该处方");
        }

        List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);
        return buildPrescriptionVO(prescription, items);
    }

    @Override
    public List<InventoryVO> listInventory(Long pharmacyId) {
        List<PharmacyInventory> inventories = pharmacyInventoryMapper.selectByPharmacyId(pharmacyId);
        return inventories.stream()
                .map(inv -> InventoryVO.builder()
                        .pharmacyId(inv.getPharmacyId())
                        .medicineName(inv.getMedicineName())
                        .stock(inv.getStock())
                        .lockStock(inv.getLockStock())
                        .unit(inv.getUnit())
                        .build())
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVO auditPrescription(Long prescriptionId, PrescriptionAuditRequest request, Long pharmacistId) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessException(ResultCode.PRESCRIPTION_NOT_FOUND);
        }
        if (prescription.getAuditStatus() != 0) {
            throw new BusinessException(ResultCode.PRESCRIPTION_ALREADY_AUDITED);
        }

        LocalDateTime now = LocalDateTime.now();
        String action = request.getAction().toUpperCase();

        switch (action) {
            case "APPROVE":
                prescriptionMapper.updateAuditStatus(prescriptionId, 1, pharmacistId,
                        request.getComments(), now);
                prescription.setAuditStatus(1);
                prescription.setPharmacistId(pharmacistId);
                prescription.setAuditComments(request.getComments());
                prescription.setAuditTime(now);
                log.info("处方审核通过: id={}, pharmacistId={}", prescriptionId, pharmacistId);
                return buildPrescriptionVO(prescription, null);

            case "REJECT":
                prescriptionMapper.updateAuditStatus(prescriptionId, 2, pharmacistId,
                        request.getComments(), now);
                List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(prescriptionId);
                for (PrescriptionItem item : items) {
                    pharmacyInventoryMapper.restoreStock(
                            item.getPharmacyId(), item.getMedicineId(), item.getQuantity());
                }
                prescription.setAuditStatus(2);
                prescription.setPharmacistId(pharmacistId);
                prescription.setAuditComments(request.getComments());
                prescription.setAuditTime(now);
                log.info("处方审核驳回: id={}, pharmacistId={}, 恢复库存{}项",
                        prescriptionId, pharmacistId, items.size());
                return buildPrescriptionVO(prescription, items);

            default:
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "不支持的审核动作: " + action);
        }
    }

    @Override
    public List<PrescriptionVO> listPendingAudit() {
        List<Prescription> prescriptions = prescriptionMapper.selectByAuditStatus(0);
        return prescriptions.stream()
                .map(p -> buildPrescriptionVO(p, null))
                .toList();
    }

    private void validateInventory(List<PrescriptionIssueRequest.MedicineItem> medicines) {
        for (PrescriptionIssueRequest.MedicineItem medicine : medicines) {
            PharmacyInventory inventory = pharmacyInventoryMapper.selectByPharmacyAndMedicine(
                    medicine.getPharmacyId(), medicine.getMedicineId()
            );
            if (inventory == null || inventory.getStock() < medicine.getQuantity()) {
                int available = inventory != null ? inventory.getStock() : 0;
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH,
                        "药品「" + medicine.getMedicineName() + "」库存不足，当前可用: " + available);
            }
        }
    }

    private PrescriptionVO buildPrescriptionVO(Prescription prescription, List<PrescriptionItem> items) {
        PrescriptionVO.PrescriptionVOBuilder builder = PrescriptionVO.builder()
                .id(prescription.getId())
                .prescriptionSn(prescription.getPrescriptionSn())
                .patientId(prescription.getPatientId())
                .doctorId(prescription.getDoctorId())
                .diagnosis(prescription.getDiagnosis())
                .pdfUrl(prescription.getPdfUrl())
                .auditStatus(prescription.getAuditStatus())
                .pharmacistId(prescription.getPharmacistId())
                .auditComments(prescription.getAuditComments())
                .auditTime(prescription.getAuditTime())
                .status(prescription.getStatus())
                .createTime(prescription.getCreateTime());

        if (items != null && !items.isEmpty()) {
            List<PrescriptionItemVO> itemVOs = items.stream()
                    .map(item -> PrescriptionItemVO.builder()
                            .medicineName(item.getMedicineName())
                            .quantity(item.getQuantity())
                            .unit(item.getUnit())
                            .build())
                    .toList();
            builder.items(itemVOs);
        }

        return builder.build();
    }
}
