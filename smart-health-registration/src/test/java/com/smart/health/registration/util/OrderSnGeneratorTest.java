package com.smart.health.registration.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSnGenerator 单元测试")
class OrderSnGeneratorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderSnGenerator orderSnGenerator;

    @Test
    @DisplayName("生成编号符合 REG_yyyyMMdd_XXXXXX 格式")
    void generate_matchesFormat() {
        // Given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        String sn = orderSnGenerator.generate();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Then
        assertThat(sn).matches("REG_\\d{8}_\\d{6}");
        assertThat(sn).startsWith("REG_" + today + "_");
        assertThat(sn).endsWith("000001");
    }

    @Test
    @DisplayName("Redis INCR 返回序号正确拼接")
    void generate_usesRedisIncr() {
        // Given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(42L);

        // When
        String sn = orderSnGenerator.generate();

        // Then
        assertThat(sn).endsWith("000042");
    }

    @Test
    @DisplayName("首次生成时设置 Key 过期时间")
    void generate_firstCall_setsExpire() {
        // Given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When
        orderSnGenerator.generate();

        // Then
        verify(stringRedisTemplate).expire(anyString(), eq(25L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("非首次调用不重复设置过期时间")
    void generate_subsequentCall_noExpire() {
        // Given
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        // When
        orderSnGenerator.generate();

        // Then - seq != 1, 不应设置过期时间
        verify(stringRedisTemplate, org.mockito.Mockito.never())
                .expire(anyString(), anyLong(), eq(TimeUnit.HOURS));
    }
}
