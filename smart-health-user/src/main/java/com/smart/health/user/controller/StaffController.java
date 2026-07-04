package com.smart.health.user.controller;

import com.smart.health.common.result.Result;
import com.smart.health.user.dto.StaffRequest;
import com.smart.health.user.dto.StaffVO;
import com.smart.health.user.service.StaffAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理控制器（仅运维人员 ADMIN 可访问）
 */
@Tag(name = "员工管理", description = "员工 CRUD（仅 ADMIN）")
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    private final StaffAuthService staffAuthService;

    @Operation(summary = "创建员工")
    @PostMapping
    public Result<StaffVO> createStaff(@Valid @RequestBody StaffRequest request) {
        return Result.ok(staffAuthService.createStaff(request));
    }

    @Operation(summary = "员工列表")
    @GetMapping
    public Result<List<StaffVO>> listStaff() {
        return Result.ok(staffAuthService.listStaff());
    }

    @Operation(summary = "员工详情")
    @GetMapping("/{id}")
    public Result<StaffVO> getStaff(@PathVariable Long id) {
        return Result.ok(staffAuthService.getStaff(id));
    }

    @Operation(summary = "更新员工信息")
    @PutMapping("/{id}")
    public Result<StaffVO> updateStaff(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return Result.ok(staffAuthService.updateStaff(id, request));
    }

    @Operation(summary = "删除员工（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStaff(@PathVariable Long id) {
        staffAuthService.deleteStaff(id);
        return Result.ok();
    }
}
