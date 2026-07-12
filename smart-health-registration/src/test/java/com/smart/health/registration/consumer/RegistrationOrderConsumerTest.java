package com.smart.health.registration.consumer;

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

    private RegistrationOrderConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RegistrationOrderConsumer(
                registrationOrderService,
                scheduleRedisConfig
        );
    }

    private SeckillOrderMessage buildOrderMessage() {
        SeckillOrderMessage orderMessage = new SeckillOrderMessage();
        orderMessage.setOrderSn("REG_20260629_000001");
        orderMessage.setPatientId(100L);
        orderMessage.setScheduleId(1L);
        orderMessage.setAmount(new BigDecimal("50.00"));
        return orderMessage;
    }

    @Test
    @DisplayName("处理消息 - 订单已存在时跳过")
    void handleOrderMessage_orderExists_skip() {
        // Given
        SeckillOrderMessage orderMessage = buildOrderMessage();
        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(new RegistrationOrder());

        // When
        consumer.handleOrderMessage(orderMessage);

        // Then
        verify(registrationOrderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("处理消息 - 成功创建订单")
    void handleOrderMessage_success() {
        // Given
        SeckillOrderMessage orderMessage = buildOrderMessage();
        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(null);
        when(registrationOrderService.createOrder(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        consumer.handleOrderMessage(orderMessage);

        // Then
        verify(registrationOrderService).createOrder(any());
    }

    @Test
    @DisplayName("处理消息 - createOrder 抛异常时仅记录日志，不抛出")
    void handleOrderMessage_createOrderThrows_logOnly() {
        // Given
        SeckillOrderMessage orderMessage = buildOrderMessage();
        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(null);
        when(registrationOrderService.createOrder(any())).thenThrow(new RuntimeException("DB error"));

        // When & Then - 不抛出异常，仅记录日志
        consumer.handleOrderMessage(orderMessage);
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
