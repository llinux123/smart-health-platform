package com.smart.health.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 首页统计服务 — 聚合跨服务的 count 查询
 * <p>
 * 通过 RestTemplate + Nacos 服务发现调用各微服务的内部统计接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RestTemplate restTemplate;

    public record PatientStats(int consultCount, int appointmentCount, int prescriptionCount) {}

    /**
     * 获取患者首页统计数据
     *
     * @param patientId 患者ID
     * @return 聚合统计（问诊数、挂号数、处方数）
     */
    public PatientStats getPatientStats(Long patientId) {
        int consultCount = callCountService("consultation-service", "/internal/consultation/stats/count", patientId);
        int appointmentCount = callCountService("registration-service", "/internal/registration/stats/count", patientId);
        int prescriptionCount = callCountService("prescription-service", "/internal/prescription/stats/count", patientId);
        return new PatientStats(consultCount, appointmentCount, prescriptionCount);
    }

    private int callCountService(String serviceName, String path, Long patientId) {
        try {
            String url = String.format("http://%s%s?patientId=%d", serviceName, path, patientId);
            Integer count = restTemplate.getForObject(url, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("调用 {} 统计接口失败: {}", serviceName, e.getMessage());
            return 0;
        }
    }
}
