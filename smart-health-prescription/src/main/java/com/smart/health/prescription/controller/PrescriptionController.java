package com.smart.health.prescription.controller;

import com.smart.health.common.result.Result;
import com.smart.health.prescription.dto.InventoryVO;
import com.smart.health.prescription.dto.PrescriptionAuditRequest;
import com.smart.health.prescription.dto.PrescriptionIssueRequest;
import com.smart.health.prescription.dto.PrescriptionVO;
import com.smart.health.prescription.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处方管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "处方管理", description = "处方开具、查询、库存管理")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/api/v1/prescription/issue")
    @Operation(summary = "开具处方", description = "创建处方并原子扣减库存")
    public Result<PrescriptionVO> issuePrescription(
            @Valid @RequestBody PrescriptionIssueRequest request,
            @RequestParam(value = "doctorId", required = false) Long doctorId) {
        Long effectiveDoctorId = doctorId != null ? doctorId : 0L;
        PrescriptionVO vo = prescriptionService.issuePrescription(request, effectiveDoctorId);
        return Result.ok(vo);
    }

    @GetMapping("/api/v1/prescriptions")
    @Operation(summary = "查询患者处方列表")
    public Result<List<PrescriptionVO>> listPrescriptions(
            @RequestParam("patientId") Long patientId) {
        List<PrescriptionVO> list = prescriptionService.listByPatient(patientId);
        return Result.ok(list);
    }

    @GetMapping("/api/v1/prescriptions/pending-audit")
    @Operation(summary = "查询待审核处方列表")
    public Result<List<PrescriptionVO>> listPendingAudit() {
        List<PrescriptionVO> list = prescriptionService.listPendingAudit();
        return Result.ok(list);
    }

    @GetMapping("/api/v1/prescriptions/{id}")
    @Operation(summary = "查询处方详情")
    public Result<PrescriptionVO> getPrescriptionDetail(
            @PathVariable("id") Long id,
            @RequestParam(value = "patientId", required = false) Long patientId) {
        PrescriptionVO vo = prescriptionService.getDetail(id, patientId);
        return Result.ok(vo);
    }

    @PostMapping("/api/v1/prescriptions/{id}/audit")
    @Operation(summary = "审核处方", description = "药师审核处方，驳回时自动恢复库存")
    public Result<PrescriptionVO> auditPrescription(
            @PathVariable("id") Long id,
            @Valid @RequestBody PrescriptionAuditRequest request,
            @RequestParam(value = "pharmacistId", required = false) Long pharmacistId) {
        Long effectivePharmacistId = pharmacistId != null ? pharmacistId : 0L;
        PrescriptionVO vo = prescriptionService.auditPrescription(id, request, effectivePharmacistId);
        return Result.ok(vo);
    }

    @GetMapping("/api/v1/pharmacy/inventory")
    @Operation(summary = "查询药房库存列表")
    public Result<List<InventoryVO>> listInventory(
            @RequestParam("pharmacyId") Long pharmacyId) {
        List<InventoryVO> list = prescriptionService.listInventory(pharmacyId);
        return Result.ok(list);
    }
}
