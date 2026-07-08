package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的短信验证码服务抽象基类。
 * <p>
 * 公共逻辑：验证码生成、Redis 存储(5min TTL)、60s 冷却、一次性消费校验、发送失败回滚。
 * 子类只需实现 {@link #deliver(String, String)} 方法完成验证码送达。
 */
@Slf4j
public abstract class AbstractRedisSmsService implements SmsService {

    private static final String SMS_KEY_PREFIX = "sms:code:";
    private static final String SMS_COOLDOWN_PREFIX = "sms:cooldown:";
    private static final long CODE_TTL = 300; // 5分钟
    private static final long COOLDOWN_SECONDS = 60; // 同一手机号发送间隔
    private static final SecureRandom RANDOM = new SecureRandom();

    protected final StringRedisTemplate redis;

    protected AbstractRedisSmsService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String sendCode(String phone) {
        String cooldownKey = SMS_COOLDOWN_PREFIX + phone;
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            log.warn("短信发送过于频繁，phone={}", phone);
            throw new BusinessException(ResultCode.SMS_SEND_FAIL, "发送太频繁，请稍后再试");
        }

        String code = generateCode();
        String key = SMS_KEY_PREFIX + phone;

        redis.opsForValue().set(key, code, CODE_TTL, TimeUnit.SECONDS);
        redis.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        try {
            deliver(phone, code);
        } catch (Exception e) {
            // 回滚 Redis，允许用户立即重试
            redis.delete(key);
            redis.delete(cooldownKey);
            log.error("短信发送失败，phone={}", phone, e);
            throw new BusinessException(ResultCode.SMS_SEND_FAIL, "短信发送失败，请稍后重试");
        }

        return null;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        String key = SMS_KEY_PREFIX + phone;
        String stored = redis.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        if (stored.equals(code)) {
            // 立即删除，一次性消费
            redis.delete(key);
            return true;
        }
        return false;
    }

    /**
     * 将验证码送达用户（打日志 / 调阿里云 API）。
     * 实现类抛出异常时，{@link #sendCode} 会回滚 Redis 并抛出 BusinessException。
     *
     * @param phone 手机号
     * @param code  验证码
     */
    protected abstract void deliver(String phone, String code);

    /**
     * 生成6位随机验证码
     */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
