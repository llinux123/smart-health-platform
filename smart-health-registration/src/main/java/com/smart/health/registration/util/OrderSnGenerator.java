package com.smart.health.registration.util;

import com.smart.health.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 挂号订单号生成器
 * 格式: REG_yyyyMMdd_XXXXXX（6位自增序号，每日重置，基于 Redis INCR 分布式生成）
 */
@Component
@RequiredArgsConstructor
public class OrderSnGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Key 过期时间：25 小时，确保跨天后自动清理 */
    private static final long KEY_EXPIRE_HOURS = 25L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成唯一订单号（基于 Redis INCR，分布式安全）
     * 格式: REG_yyyyMMdd_XXXXXX
     *
     * @return 唯一订单号
     */
    public String generate() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String redisKey = CommonConstants.ORDER_SN_COUNTER_PREFIX + today;

        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq == 1L) {
            // 首次创建，设置过期时间
            stringRedisTemplate.expire(redisKey, KEY_EXPIRE_HOURS, TimeUnit.HOURS);
        }

        return CommonConstants.ORDER_SN_PREFIX + today + "_" + String.format("%06d", seq);
    }
}
