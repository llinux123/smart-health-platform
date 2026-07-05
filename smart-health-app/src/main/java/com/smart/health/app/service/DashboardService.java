package com.smart.health.app.service;

import com.smart.health.consultation.service.SessionAccessor;
import com.smart.health.prescription.service.PrescriptionService;
import com.smart.health.registration.service.RegistrationOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 首页统计服务 — 聚合模式：直接注入各子模块 Service 并行查询
 * <p>
 * 替代原先基于 RestTemplate 跨服务 HTTP 调用的方案，
 * 消除部署模式混淆、网络超时和串行阻塞问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SessionAccessor sessionAccessor;
    private final RegistrationOrderService registrationOrderService;
    private final PrescriptionService prescriptionService;

    /** 专用于并行统计查询的线程池（3 条 COUNT 查询，无需大量线程） */
    private static final ExecutorService STATS_EXECUTOR = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "dashboard-stats");
        t.setDaemon(true);
        return t;
    });

    public record PatientStats(int consultCount, int appointmentCount, int prescriptionCount) {}

    /**
     * 获取患者首页统计数据
     * <p>
     * 三个 COUNT 查询并行执行，最长等待 10 秒后超时降级。
     *
     * @param patientId 患者ID
     * @return 聚合统计（问诊数、挂号数、处方数）
     */
    public PatientStats getPatientStats(Long patientId) {
        CompletableFuture<Integer> consultFuture = CompletableFuture.supplyAsync(
                () -> queryCount("问诊", () -> sessionAccessor.countByPatientId(patientId)),
                STATS_EXECUTOR);

        CompletableFuture<Integer> appointmentFuture = CompletableFuture.supplyAsync(
                () -> queryCount("挂号", () -> registrationOrderService.countByPatientId(patientId)),
                STATS_EXECUTOR);

        CompletableFuture<Integer> prescriptionFuture = CompletableFuture.supplyAsync(
                () -> queryCount("处方", () -> prescriptionService.countByPatientId(patientId)),
                STATS_EXECUTOR);

        try {
            CompletableFuture.allOf(consultFuture, appointmentFuture, prescriptionFuture)
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Dashboard 统计查询超时", e);
        }

        int consultCount = getOrDefault(consultFuture, "问诊");
        int appointmentCount = getOrDefault(appointmentFuture, "挂号");
        int prescriptionCount = getOrDefault(prescriptionFuture, "处方");

        return new PatientStats(consultCount, appointmentCount, prescriptionCount);
    }

    private int queryCount(String label, java.util.function.IntSupplier supplier) {
        try {
            return supplier.getAsInt();
        } catch (Exception e) {
            log.error("Dashboard {} 统计查询失败: {}", label, e.getMessage());
            return 0;
        }
    }

    private int getOrDefault(CompletableFuture<Integer> future, String label) {
        try {
            Integer result = future.getNow(0);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Dashboard {} 统计结果获取失败: {}", label, e.getMessage());
            return 0;
        }
    }
}
