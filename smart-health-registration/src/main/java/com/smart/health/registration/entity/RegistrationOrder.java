package com.smart.health.registration.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号订单实体
 * 对应表: t_registration_order
 */
@Data
public class RegistrationOrder {

    /** 挂号单ID */
    private Long id;

    /** 订单流水号(全局唯一) */
    private String orderSn;

    /** 患者ID */
    private Long patientId;

    /** 排班ID */
    private Long scheduleId;

    /** 就诊呼叫序号 */
    private Integer sequenceNumber;

    /** 支付金额 */
    private BigDecimal amount;

    /** 状态 (0:排队中 1:待支付 2:已支付 3:已就诊 4:已退号) */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
