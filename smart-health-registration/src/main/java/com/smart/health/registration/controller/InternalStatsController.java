package com.smart.health.registration.controller;

import com.smart.health.registration.service.RegistrationOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部统计接口 — 供其他微服务通过 HTTP 调用
 */
@RestController("internalStatsRegistrationController")
@RequiredArgsConstructor
public class InternalStatsController {

    private final RegistrationOrderService registrationOrderService;

    @GetMapping("/internal/registration/stats/count")
    public int countByPatientId(@RequestParam Long patientId) {
        return registrationOrderService.countByPatientId(patientId);
    }
}
