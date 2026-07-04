package com.smart.health.consultation.controller;

import com.smart.health.consultation.service.SessionAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部统计接口 — 供其他微服务通过 HTTP 调用
 */
@RestController("internalStatsConsultationController")
@RequiredArgsConstructor
public class InternalStatsController {

    private final SessionAccessor sessionAccessor;

    @GetMapping("/internal/consultation/stats/count")
    public int countByPatientId(@RequestParam Long patientId) {
        return sessionAccessor.countByPatientId(patientId);
    }
}
