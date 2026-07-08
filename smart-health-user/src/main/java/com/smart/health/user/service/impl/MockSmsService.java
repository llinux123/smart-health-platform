package com.smart.health.user.service.impl;

import com.smart.health.user.service.AbstractRedisSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Mock 短信验证码服务
 * <p>
 * 开发/演示环境：验证码生成后打印到日志（仅显示后3位）。
 * 默认生效（sms.provider 未配置或为 "mock" 时激活）。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsService extends AbstractRedisSmsService {

    public MockSmsService(StringRedisTemplate redis) {
        super(redis);
    }

    @Override
    protected void deliver(String phone, String code) {
        log.info("========== 短信验证码（Mock） ==========");
        log.info("手机号: {}", phone);
        log.info("验证码: ***{}", code.substring(3));
        log.info("有效期: 300 秒");
        log.info("=======================================");
    }
}
