package com.smart.health.registration.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀订单消息体
 * 用于 RabbitMQ 异步创建挂号订单
 */
@Data
public class SeckillOrderMessage {
    
    /** 订单流水号 */
    private String orderSn;
    
    /** 患者ID */
    private Long patientId;
    
    /** 排班ID */
    private Long scheduleId;
    
    /** 支付金额 */
    private BigDecimal amount;
}