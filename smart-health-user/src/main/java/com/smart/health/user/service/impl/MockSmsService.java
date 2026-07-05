package com.smart.health.user.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.user.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Mock 短信验证码服务
 * <p>
 * 开发/演示环境：生成验证码后打印到日志 + 存入 Redis
 * 后续接入阿里云SMS只需新增一个 AliyunSmsService 实现即可
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockSmsService implements SmsService {

    private static final String SMS_KEY_PREFIX = "sms:code:";
    private static final String SMS_COOLDOWN_PREFIX = "sms:cooldown:";
    private static final long CODE_TTL = 300; // 5分钟
    private static final long COOLDOWN_SECONDS = 60; // 同一手机号发送间隔
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String sendCode(String phone) {
        String cooldownKey = SMS_COOLDOWN_PREFIX + phone;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            log.warn("短信发送过于频繁，phone={}", phone);
            throw new BusinessException(ResultCode.SMS_SEND_FAIL, "发送太频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = SMS_KEY_PREFIX + phone;

        stringRedisTemplate.opsForValue().set(key, code, CODE_TTL, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        log.info("========== 短信验证码（Mock） ==========");
        log.info("手机号: {}", phone);
        log.info("验证码: ***{}", code.substring(3));
        log.info("有效期: {} 秒", CODE_TTL);
        log.info("=======================================");

        return code;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        String key = SMS_KEY_PREFIX + phone;
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        if (stored.equals(code)) {
            // 立即删除，一次性消费
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
