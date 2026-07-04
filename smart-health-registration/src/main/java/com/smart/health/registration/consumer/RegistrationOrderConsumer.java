package com.smart.health.registration.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.constant.CommonConstants;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.SeckillOrderMessage;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.service.RegistrationOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 挂号订单消费者
 * 监听 MQ 消息，异步创建挂号订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationOrderConsumer {

    private final RegistrationOrderService registrationOrderService;
    private final ScheduleRedisConfig scheduleRedisConfig;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = CommonConstants.MQ_QUEUE_REGISTRATION_ORDER)
    public void handleOrderMessage(String message) {
        log.info("收到挂号订单消息：{}", message);

        try {
            SeckillOrderMessage orderMessage = objectMapper.readValue(message, SeckillOrderMessage.class);

            // 幂等性检查：订单是否已创建
            RegistrationOrder existingOrder = registrationOrderService.getByOrderSn(orderMessage.getOrderSn());
            if (existingOrder != null) {
                log.warn("订单已存在，跳过创建，orderSn={}", orderMessage.getOrderSn());
                return;
            }

            // 创建订单
            RegistrationOrder order = new RegistrationOrder();
            order.setOrderSn(orderMessage.getOrderSn());
            order.setPatientId(orderMessage.getPatientId());
            order.setScheduleId(orderMessage.getScheduleId());
            order.setAmount(orderMessage.getAmount());

            registrationOrderService.createOrder(order);
            log.info("挂号订单创建成功，orderSn={}", orderMessage.getOrderSn());

        } catch (Exception e) {
            log.error("处理挂号订单消息失败，message={}", message, e);
            // 不再重新抛出异常，避免 RabbitMQ 无限重试
            // 幂等性检查保证下次手动重试时不会重复创建订单
        }
    }

    @RabbitListener(queues = CommonConstants.MQ_DLQ_REGISTRATION)
    public void handleDeadLetterMessage(String message) {
        log.error("收到死信队列消息，需要人工处理：{}", message);
        // 死信队列消息处理：记录日志，可接入告警系统
        // 后续可扩展重试机制或人工干预流程
    }
}
