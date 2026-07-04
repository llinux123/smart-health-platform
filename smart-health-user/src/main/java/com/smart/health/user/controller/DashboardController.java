package com.smart.health.user.controller;

import com.smart.health.common.result.Result;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.user.service.DashboardService;
import com.smart.health.user.service.DashboardService.PatientStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页统计控制器
 */
@Tag(name = "首页统计", description = "患者首页聚合统计数据")
@RestController
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/v1/dashboard/stats")
    @Operation(summary = "获取患者首页统计数据", description = "聚合问诊数、挂号数、处方数")
    public Result<PatientStats> getStats() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        PatientStats stats = dashboardService.getPatientStats(patientId);
        return Result.ok(stats);
    }
}
