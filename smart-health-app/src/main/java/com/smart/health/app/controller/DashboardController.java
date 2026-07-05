package com.smart.health.app.controller;

import com.smart.health.app.service.DashboardService;
import com.smart.health.app.service.DashboardService.PatientStats;
import com.smart.health.common.result.Result;
import com.smart.health.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页统计控制器（聚合模式）
 * <p>
 * 直接注入各子模块的 Service 进行并行 COUNT 查询，
 * 替代原先跨服务 HTTP 调用的方案。
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
