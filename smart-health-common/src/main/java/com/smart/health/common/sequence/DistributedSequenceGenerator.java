package com.smart.health.common.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 分布式序列号生成器 — 基于 Redis INCR，每日自动重置
 * <p>
 * 各业务模块通过不同的 keyPrefix 获取独立的序列号，
 * 共享同一套 Redis 分布式安全机制。
 */
@Component
@RequiredArgsConstructor
public class DistributedSequenceGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Key 过期时间：25 小时，确保跨天后自动清理 */
    private static final long KEY_EXPIRE_HOURS = 25L;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取下一个序列号（每日重置）
     *
     * @param keyPrefix Redis key 前缀，如 "sn:counter:order:"
     * @return 当日递增序号（从 1 开始）
     */
    public long next(String keyPrefix) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String redisKey = keyPrefix + today;

        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1L) {
            stringRedisTemplate.expire(redisKey, KEY_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return seq != null ? seq : 0L;
    }

    /**
     * 获取格式化的序列号（6 位补零）
     *
     * @param keyPrefix Redis key 前缀
     * @return 格式化后的 6 位序号字符串，如 "000001"
     */
    public String nextFormatted(String keyPrefix) {
        return String.format("%06d", next(keyPrefix));
    }
}
