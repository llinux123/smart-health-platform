package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractRedisEmailService 抽象基类单元测试")
class AbstractRedisEmailServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    private String deliveredEmail;
    private String deliveredCode;
    private boolean deliverShouldFail;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        deliveredEmail = null;
        deliveredCode = null;
        deliverShouldFail = false;
    }

    private AbstractRedisEmailService createService() {
        return new AbstractRedisEmailService(redis) {
            @Override
            protected void deliver(String email, String code) {
                deliveredEmail = email;
                deliveredCode = code;
                if (deliverShouldFail) {
                    throw new RuntimeException("deliver failed");
                }
            }
        };
    }

    @Test
    @DisplayName("sendCode 正常发送 - 存入Redis并调用deliver")
    @SuppressWarnings("unchecked")
    void sendCode_normal_storesCodeAndCallsDeliver() {
        String email = "test@example.com";
        when(redis.hasKey("email:cooldown:" + email)).thenReturn(false);
        AbstractRedisEmailService service = createService();

        String result = service.sendCode(email);

        assertThat(result).isNull();
        assertThat(deliveredEmail).isEqualTo(email);
        assertThat(deliveredCode).hasSize(6);
        verify(valueOps).set(eq("email:code:" + email), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq("email:cooldown:" + email), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("sendCode 冷却期内 - 抛BusinessException")
    void sendCode_inCooldown_throwsBusinessException() {
        String email = "test@example.com";
        when(redis.hasKey("email:cooldown:" + email)).thenReturn(true);

        AbstractRedisEmailService service = createService();

        assertThatThrownBy(() -> service.sendCode(email))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.EMAIL_SEND_FAIL.getCode());

        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
        assertThat(deliveredEmail).isNull();
    }

    @Test
    @DisplayName("sendCode deliver失败 - 回滚Redis并抛BusinessException")
    @SuppressWarnings("unchecked")
    void sendCode_deliverFails_rollsBackRedisAndThrows() {
        String email = "test@example.com";
        when(redis.hasKey("email:cooldown:" + email)).thenReturn(false);
        deliverShouldFail = true;

        AbstractRedisEmailService service = createService();

        assertThatThrownBy(() -> service.sendCode(email))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.EMAIL_SEND_FAIL.getCode());

        verify(redis).delete("email:code:" + email);
        verify(redis).delete("email:cooldown:" + email);
        assertThat(deliveredEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("verifyCode 正确 - 删除key返回true")
    @SuppressWarnings("unchecked")
    void verifyCode_correct_deletesKeyAndReturnsTrue() {
        String email = "test@example.com";
        String code = "123456";
        when(valueOps.get("email:code:" + email)).thenReturn(code);

        AbstractRedisEmailService service = createService();

        boolean result = service.verifyCode(email, code);

        assertThat(result).isTrue();
        verify(redis).delete("email:code:" + email);
    }

    @Test
    @DisplayName("verifyCode 错误 - 不删除key返回false")
    @SuppressWarnings("unchecked")
    void verifyCode_wrong_doesNotDeleteAndReturnsFalse() {
        String email = "test@example.com";
        when(valueOps.get("email:code:" + email)).thenReturn("654321");

        AbstractRedisEmailService service = createService();

        boolean result = service.verifyCode(email, "000000");

        assertThat(result).isFalse();
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("verifyCode 已过期 - key不存在返回false")
    @SuppressWarnings("unchecked")
    void verifyCode_expired_keyNotFoundReturnsFalse() {
        String email = "test@example.com";
        when(valueOps.get("email:code:" + email)).thenReturn(null);

        AbstractRedisEmailService service = createService();

        boolean result = service.verifyCode(email, "123456");

        assertThat(result).isFalse();
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("verifyCode 参数为null - 直接返回false")
    void verifyCode_nullArgs_returnsFalse() {
        AbstractRedisEmailService service = createService();

        assertThat(service.verifyCode(null, "123456")).isFalse();
        assertThat(service.verifyCode("test@example.com", null)).isFalse();
    }
}
