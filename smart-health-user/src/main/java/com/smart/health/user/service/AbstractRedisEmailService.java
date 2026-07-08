package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractRedisEmailService implements EmailService {

    private static final String EMAIL_KEY_PREFIX = "email:code:";
    private static final String EMAIL_COOLDOWN_PREFIX = "email:cooldown:";
    private static final long CODE_TTL = 300;
    private static final long COOLDOWN_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    protected final StringRedisTemplate redis;

    protected AbstractRedisEmailService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String sendCode(String email) {
        String cooldownKey = EMAIL_COOLDOWN_PREFIX + email;
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            log.warn("邮箱验证码发送过于频繁，email={}", email);
            throw new BusinessException(ResultCode.EMAIL_SEND_FAIL, "发送太频繁，请稍后再试");
        }

        String code = generateCode();
        String key = EMAIL_KEY_PREFIX + email;

        redis.opsForValue().set(key, code, CODE_TTL, TimeUnit.SECONDS);
        redis.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        try {
            deliver(email, code);
        } catch (Exception e) {
            redis.delete(key);
            redis.delete(cooldownKey);
            log.error("邮箱验证码发送失败，email={}", email, e);
            throw new BusinessException(ResultCode.EMAIL_SEND_FAIL, "验证码发送失败，请稍后重试");
        }

        return null;
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        String key = EMAIL_KEY_PREFIX + email;
        String stored = redis.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        if (stored.equals(code)) {
            redis.delete(key);
            return true;
        }
        return false;
    }

    protected abstract void deliver(String email, String code);

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
