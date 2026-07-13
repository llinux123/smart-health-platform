package com.smart.health.prescription.controller;

import com.smart.health.common.result.Result;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.prescription.dto.InboundRequest;
import com.smart.health.prescription.dto.OutboundRequest;
import com.smart.health.prescription.dto.ReconcileRequest;
import com.smart.health.prescription.entity.InventoryLog;
import com.smart.health.prescription.entity.PharmacyInventory;
import com.smart.health.prescription.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 药房库存控制器
 */
@Tag(name = "药房库存", description = "药品入库、出库、盘点及库存查询")
@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "药品入库")
    @PostMapping("/api/v1/inventory/inbound")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> inbound(@Valid @RequestBody InboundRequest request) {
        Long operatorId = SecurityUtils.getCurrentStaffId();
        inventoryService.inbound(request, operatorId);
        return Result.ok();
    }

    @Operation(summary = "药品出库")
    @PostMapping("/api/v1/inventory/outbound")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> outbound(@Valid @RequestBody OutboundRequest request) {
        Long operatorId = SecurityUtils.getCurrentStaffId();
        inventoryService.outbound(request, operatorId);
        return Result.ok();
    }

    @Operation(summary = "库存盘点")
    @PostMapping("/api/v1/inventory/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reconcile(@Valid @RequestBody ReconcileRequest request) {
        Long operatorId = SecurityUtils.getCurrentStaffId();
        inventoryService.reconcile(request, operatorId);
        return Result.ok();
    }

    @Operation(summary = "查询药房库存列表")
    @GetMapping("/api/v1/inventory/list")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public Result<List<PharmacyInventory>> list(
            @Parameter(description = "药房ID") @RequestParam(defaultValue = "1") Long pharmacyId) {
        String role = SecurityUtils.getCurrentRole();
        if ("ADMIN".equals(role)) {
            return Result.ok(inventoryService.listAll());
        }
        return Result.ok(inventoryService.listByPharmacy(pharmacyId));
    }

    @Operation(summary = "查询库存变动日志")
    @GetMapping("/api/v1/inventory/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<InventoryLog>> logs(
            @Parameter(description = "药房ID") @RequestParam(required = false) Long pharmacyId,
            @Parameter(description = "药品ID") @RequestParam(required = false) Long medicineId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(inventoryService.listLogs(pharmacyId, medicineId, page, size));
    }
}
