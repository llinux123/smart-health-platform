package com.smart.health.registration.config;

import com.smart.health.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 排班 Redis 配置
 * 提供 Redis 库存初始化能力
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScheduleRedisConfig {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化排班 Redis 库存
     *
     * @param scheduleId 排班ID
     * @param count      库存数量
     */
    public void initScheduleStock(Long scheduleId, int count) {
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(count));
        log.info("初始化排班Redis库存，scheduleId={}, count={}", scheduleId, count);
    }

    /**
     * 获取当前 Redis 库存
     *
     * @param scheduleId 排班ID
     * @return 库存数量，不存在时返回 null
     */
    public Long getScheduleStock(Long scheduleId) {
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        String value = stringRedisTemplate.opsForValue().get(stockKey);
        return value != null ? Long.parseLong(value) : null;
    }
}
