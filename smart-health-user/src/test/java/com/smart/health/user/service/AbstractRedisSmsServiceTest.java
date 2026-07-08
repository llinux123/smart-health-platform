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
@DisplayName("AbstractRedisSmsService 抽象基类单元测试")
class AbstractRedisSmsServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    private String deliveredPhone;
    private String deliveredCode;
    private boolean deliverShouldFail;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        deliveredPhone = null;
        deliveredCode = null;
        deliverShouldFail = false;
    }

    /** 创建测试用子类，deliver 记录参数或抛异常 */
    private AbstractRedisSmsService createService() {
        return new AbstractRedisSmsService(redis) {
            @Override
            protected void deliver(String phone, String code) {
                deliveredPhone = phone;
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
        // Given
        String phone = "13800138000";
        when(redis.hasKey("sms:cooldown:" + phone)).thenReturn(false);
        AbstractRedisSmsService service = createService();

        // When
        String result = service.sendCode(phone);

        // Then
        assertThat(result).isNull();
        assertThat(deliveredPhone).isEqualTo(phone);
        assertThat(deliveredCode).hasSize(6);
        // 验证存入 Redis（code + cooldown）
        verify(valueOps).set(eq("sms:code:" + phone), anyString(), eq(300L), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq("sms:cooldown:" + phone), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("sendCode 冷却期内 - 抛BusinessException")
    void sendCode_inCooldown_throwsBusinessException() {
        // Given
        String phone = "13800138000";
        when(redis.hasKey("sms:cooldown:" + phone)).thenReturn(true);

        AbstractRedisSmsService service = createService();

        // When & Then
        assertThatThrownBy(() -> service.sendCode(phone))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SMS_SEND_FAIL.getCode());

        // 验证没有存入 Redis
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any());
        assertThat(deliveredPhone).isNull();
    }

    @Test
    @DisplayName("sendCode deliver失败 - 回滚Redis并抛BusinessException")
    @SuppressWarnings("unchecked")
    void sendCode_deliverFails_rollsBackRedisAndThrows() {
        // Given
        String phone = "13800138000";
        when(redis.hasKey("sms:cooldown:" + phone)).thenReturn(false);
        deliverShouldFail = true;

        AbstractRedisSmsService service = createService();

        // When & Then
        assertThatThrownBy(() -> service.sendCode(phone))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SMS_SEND_FAIL.getCode());

        // 验证回滚：删除 code key 和 cooldown key
        verify(redis).delete("sms:code:" + phone);
        verify(redis).delete("sms:cooldown:" + phone);
        // deliver 被调用了
        assertThat(deliveredPhone).isEqualTo(phone);
    }

    @Test
    @DisplayName("verifyCode 正确 - 删除key返回true")
    @SuppressWarnings("unchecked")
    void verifyCode_correct_deletesKeyAndReturnsTrue() {
        // Given
        String phone = "13800138000";
        String code = "123456";
        when(valueOps.get("sms:code:" + phone)).thenReturn(code);

        AbstractRedisSmsService service = createService();

        // When
        boolean result = service.verifyCode(phone, code);

        // Then
        assertThat(result).isTrue();
        verify(redis).delete("sms:code:" + phone);
    }

    @Test
    @DisplayName("verifyCode 错误 - 不删除key返回false")
    @SuppressWarnings("unchecked")
    void verifyCode_wrong_doesNotDeleteAndReturnsFalse() {
        // Given
        String phone = "13800138000";
        when(valueOps.get("sms:code:" + phone)).thenReturn("654321");

        AbstractRedisSmsService service = createService();

        // When
        boolean result = service.verifyCode(phone, "000000");

        // Then
        assertThat(result).isFalse();
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("verifyCode 已过期 - key不存在返回false")
    @SuppressWarnings("unchecked")
    void verifyCode_expired_keyNotFoundReturnsFalse() {
        // Given
        String phone = "13800138000";
        when(valueOps.get("sms:code:" + phone)).thenReturn(null);

        AbstractRedisSmsService service = createService();

        // When
        boolean result = service.verifyCode(phone, "123456");

        // Then
        assertThat(result).isFalse();
        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("verifyCode 参数为null - 直接返回false")
    void verifyCode_nullArgs_returnsFalse() {
        AbstractRedisSmsService service = createService();

        assertThat(service.verifyCode(null, "123456")).isFalse();
        assertThat(service.verifyCode("13800138000", null)).isFalse();
    }
}
