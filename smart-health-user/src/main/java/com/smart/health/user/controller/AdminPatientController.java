package com.smart.health.user.controller;

import com.smart.health.common.result.Result;
import com.smart.health.user.dto.RegisterRequest;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.service.PatientAuthService;
import com.smart.health.user.service.PatientManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者管理控制器（仅运维人员 ADMIN 可访问）
 */
@Tag(name = "患者管理", description = "患者 CRUD（仅 ADMIN）")
@RestController
@RequestMapping("/api/v1/admin/patients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPatientController {

    private final PatientManageService patientManageService;
    private final PatientAuthService patientAuthService;

    @Operation(summary = "患者列表")
    @GetMapping
    public Result<List<Patient>> listPatients() {
        return Result.ok(patientManageService.listAll());
    }

    @Operation(summary = "患者详情")
    @GetMapping("/{id}")
    public Result<Patient> getPatient(@PathVariable Long id) {
        return Result.ok(patientManageService.getById(id));
    }

    @Operation(summary = "添加患者")
    @PostMapping
    public Result<Void> addPatient(@Valid @RequestBody RegisterRequest request) {
        patientAuthService.register(request);
        return Result.ok();
    }

    @Operation(summary = "编辑患者信息")
    @PutMapping("/{id}")
    public Result<Void> updatePatient(@PathVariable Long id, @RequestBody Patient patient) {
        patientManageService.update(id, patient);
        return Result.ok();
    }

    @Operation(summary = "删除患者（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deletePatient(@PathVariable Long id) {
        patientManageService.delete(id);
        return Result.ok();
    }
}
