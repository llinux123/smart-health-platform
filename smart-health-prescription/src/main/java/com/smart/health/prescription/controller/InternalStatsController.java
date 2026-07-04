package com.smart.health.prescription.controller;

import com.smart.health.prescription.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部统计接口 — 供其他微服务通过 HTTP 调用
 */
@RestController("internalStatsPrescriptionController")
@RequiredArgsConstructor
public class InternalStatsController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/internal/prescription/stats/count")
    public int countByPatientId(@RequestParam Long patientId) {
        return prescriptionService.countByPatientId(patientId);
    }
}
