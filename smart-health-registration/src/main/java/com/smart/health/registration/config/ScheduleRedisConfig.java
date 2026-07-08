package com.smart.health.registration.config;

import com.smart.health.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * 排班 Redis 配置
 * 提供 Redis 库存、价格、分布式锁、幂等性检查能力
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ScheduleRedisConfig {

    private static final String SCHEDULE_PRICE_KEY_PREFIX = "schedule:price:";
    private static final long SCHEDULE_PRICE_TTL_HOURS = 24L;
    private static final long SECKILL_SET_TTL_MINUTES = 10L;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 初始化排班 Redis 库存与价格（写穿透，保证秒杀热路径无需访问 DB）
     *
     * @param scheduleId 排班ID
     * @param count      库存数量
     * @param price      挂号费
     */
    public void initScheduleStock(Long scheduleId, int count, BigDecimal price) {
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        String priceKey = SCHEDULE_PRICE_KEY_PREFIX + scheduleId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(count));
        if (price != null) {
            stringRedisTemplate.opsForValue().set(priceKey, price.toPlainString(), SCHEDULE_PRICE_TTL_HOURS, TimeUnit.HOURS);
        }
        log.info("初始化排班Redis缓存，scheduleId={}, stock={}, price={}", scheduleId, count, price);
    }

    /**
     * 获取 Redis 缓存的挂号费
     *
     * @param scheduleId 排班ID
     * @return 挂号费，未缓存时返回 null
     */
    public BigDecimal getSchedulePrice(Long scheduleId) {
        String priceKey = SCHEDULE_PRICE_KEY_PREFIX + scheduleId;
        String value = stringRedisTemplate.opsForValue().get(priceKey);
        return value != null ? new BigDecimal(value) : null;
    }

    /**
     * 手动写入价格缓存（用于回填或补偿）
     */
    public void setSchedulePrice(Long scheduleId, BigDecimal price) {
        if (price == null) {
            return;
        }
        String priceKey = SCHEDULE_PRICE_KEY_PREFIX + scheduleId;
        stringRedisTemplate.opsForValue().set(priceKey, price.toPlainString(), SCHEDULE_PRICE_TTL_HOURS, TimeUnit.HOURS);
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

    /**
     * Redis 原子预扣库存
     *
     * @param scheduleId 排班ID
     * @return 扣减后剩余库存，null 表示 key 不存在
     */
    public Long decrementStock(Long scheduleId) {
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        return stringRedisTemplate.opsForValue().decrement(stockKey);
    }

    /**
     * Redis 回滚库存（INCR）
     *
     * @param scheduleId 排班ID
     */
    public void incrementStock(Long scheduleId) {
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        stringRedisTemplate.opsForValue().increment(stockKey);
    }

    /**
     * 尝试获取秒杀分布式锁
     *
     * @param scheduleId   排班ID
     * @param patientId    患者ID
     * @param waitTimeSec  等待时间（秒）
     * @param leaseTimeSec 持有时间（秒）
     * @return 锁对象，获取失败返回 null
     */
    public RLock tryAcquireSeckillLock(Long scheduleId, Long patientId,
                                        long waitTimeSec, long leaseTimeSec) {
        String lockKey = CommonConstants.REDIS_SECKILL_LOCK_PREFIX + scheduleId + ":" + patientId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTimeSec, leaseTimeSec, TimeUnit.SECONDS)) {
                return lock;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * 检查患者是否已抢过该排班（幂等性检查）
     *
     * @param scheduleId 排班ID
     * @param patientId  患者ID
     * @return true 表示已抢过
     */
    public boolean isPatientInSeckillSet(Long scheduleId, Long patientId) {
        String key = CommonConstants.REDIS_SECKILL_LOCK_PREFIX + scheduleId;
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(key, String.valueOf(patientId)));
    }

    /**
     * 将患者加入已抢集合（原子操作）
     *
     * @param scheduleId 排班ID
     * @param patientId  患者ID
     * @return true 表示成功加入（首次抢号），false 表示已存在
     */
    public boolean addPatientToSeckillSet(Long scheduleId, Long patientId) {
        String key = CommonConstants.REDIS_SECKILL_LOCK_PREFIX + scheduleId;
        Long added = stringRedisTemplate.opsForSet().add(key, String.valueOf(patientId));
        if (added != null && added > 0) {
            stringRedisTemplate.expire(key, SECKILL_SET_TTL_MINUTES, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }

    /**
     * 从已抢集合中移除患者（回滚用）
     *
     * @param scheduleId 排班ID
     * @param patientId  患者ID
     */
    public void removePatientFromSeckillSet(Long scheduleId, Long patientId) {
        String key = CommonConstants.REDIS_SECKILL_LOCK_PREFIX + scheduleId;
        stringRedisTemplate.opsForSet().remove(key, String.valueOf(patientId));
    }
}
