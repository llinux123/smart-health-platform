package com.smart.health.consultation.task;

import com.smart.health.consultation.service.SessionArchive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 问诊定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationScheduledTasks {

    private final SessionArchive sessionArchive;

    /**
     * 每天凌晨2点清理回收站中超过30天的记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredRecycleBin() {
        log.info("开始执行回收站自动清理任务");
        try {
            sessionArchive.cleanExpiredRecycleBin();
            log.info("回收站自动清理任务完成");
        } catch (Exception e) {
            log.error("回收站自动清理任务异常", e);
        }
    }
}
