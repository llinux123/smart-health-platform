package com.smart.health.user.service.impl;

import com.smart.health.user.service.AbstractRedisEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService extends AbstractRedisEmailService {

    public MockEmailService(StringRedisTemplate redis) {
        super(redis);
    }

    @Override
    protected void deliver(String email, String code) {
        log.info("========== 邮箱验证码（Mock） ==========");
        log.info("邮箱: {}", email);
        log.info("验证码: {}", code);
        log.info("有效期: 300 秒");
        log.info("=======================================");
    }
}
