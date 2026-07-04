package com.smart.health.registration.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.SeckillOrderMessage;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.service.RegistrationOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationOrderConsumer 单元测试")
class RegistrationOrderConsumerTest {

    @Mock
    private RegistrationOrderService registrationOrderService;

    @Mock
    private ScheduleRedisConfig scheduleRedisConfig;

    @Mock
    private ObjectMapper objectMapper;

    private RegistrationOrderConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RegistrationOrderConsumer(
                registrationOrderService,
                scheduleRedisConfig,
                objectMapper
        );
    }

    @Test
    @DisplayName("处理消息 - 订单已存在时跳过")
    void handleOrderMessage_orderExists_skip() throws Exception {
        // Given
        String message = "{\"orderSn\":\"REG_20260629_000001\",\"patientId\":100,\"scheduleId\":1,\"amount\":50.0}";
        SeckillOrderMessage orderMessage = new SeckillOrderMessage();
        orderMessage.setOrderSn("REG_20260629_000001");
        orderMessage.setPatientId(100L);
        orderMessage.setScheduleId(1L);
        orderMessage.setAmount(new BigDecimal("50.00"));

        when(objectMapper.readValue(message, SeckillOrderMessage.class)).thenReturn(orderMessage);
        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(new RegistrationOrder());

        // When
        consumer.handleOrderMessage(message);

        // Then
        verify(registrationOrderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("处理消息 - 成功创建订单")
    void handleOrderMessage_success() throws Exception {
        // Given
        String message = "{\"orderSn\":\"REG_20260629_000001\",\"patientId\":100,\"scheduleId\":1,\"amount\":50.0}";
        SeckillOrderMessage orderMessage = new SeckillOrderMessage();
        orderMessage.setOrderSn("REG_20260629_000001");
        orderMessage.setPatientId(100L);
        orderMessage.setScheduleId(1L);
        orderMessage.setAmount(new BigDecimal("50.00"));

        when(objectMapper.readValue(message, SeckillOrderMessage.class)).thenReturn(orderMessage);
        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(null);
        when(registrationOrderService.createOrder(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.handleOrderMessage(message);

        // Then
        verify(registrationOrderService).createOrder(any());
    }

    @Test
    @DisplayName("处理消息 - 解析失败仅记录日志，不抛出异常")
    void handleOrderMessage_parseError_logOnly() throws Exception {
        // Given
        String message = "invalid json";
        when(objectMapper.readValue(anyString(), any(Class.class))).thenThrow(new RuntimeException("Parse error"));

        // When & Then - 不抛出异常，仅记录日志
        consumer.handleOrderMessage(message);
    }

    @Test
    @DisplayName("处理死信队列消息 - 记录日志")
    void handleDeadLetterMessage_logOnly() {
        // Given
        String message = "{\"orderSn\":\"REG_20260629_000001\"}";

        // When
        consumer.handleDeadLetterMessage(message);

        // Then - 仅记录日志，不抛出异常
    }
}
