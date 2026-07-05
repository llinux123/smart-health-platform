package com.smart.health.prescription.controller;

import com.smart.health.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部统计接口 — 供其他微服务通过 HTTP 调用
 * <p>
 * 仅限微服务拆分部署模式使用；聚合模式下由 {@code DashboardService} 直接注入 Service 调用。
 * 该端点不对外暴露（网关仅路由 /api/v1/** 路径）。
 */
@RestController("internalStatsPrescriptionController")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class InternalStatsController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/internal/prescription/stats/count")
    public int countByPatientId(@RequestParam Long patientId) {
        return prescriptionService.countByPatientId(patientId);
    }
}
