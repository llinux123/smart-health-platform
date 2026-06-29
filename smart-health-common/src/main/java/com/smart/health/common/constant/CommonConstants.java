package com.smart.health.common.constant;

/**
 * 系统常量
 */
public class CommonConstants {

    /** JWT Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** JWT Header */
    public static final String TOKEN_HEADER = "Authorization";

    /** Redis Key 前缀 - 挂号库存 */
    public static final String REDIS_SCHEDULE_STOCK_PREFIX = "schedule:stock:";

    /** Redis Key 前缀 - 抢号锁 */
    public static final String REDIS_SECKILL_LOCK_PREFIX = "seckill:lock:";

    /** RabbitMQ 交换机 */
    public static final String MQ_EXCHANGE_REGISTRATION = "exchange.registration";

    /** RabbitMQ 队列 - 挂号订单 */
    public static final String MQ_QUEUE_REGISTRATION_ORDER = "queue.registration.order";

    /** RabbitMQ Routing Key */
    public static final String MQ_ROUTING_KEY_ORDER_CREATE = "order.create";

    /** 挂号订单号前缀 */
    public static final String ORDER_SN_PREFIX = "REG_";

    /** 处方编号前缀（旧格式） */
    public static final String PRESCRIPTION_SN_PREFIX = "PRE_";

    /** 处方编号前缀（新格式: RX_yyyyMMdd_hospitalId_XXXXXX） */
    public static final String RX_SN_PREFIX = "RX_";

    /** 默认院区/医院ID */
    public static final String DEFAULT_HOSPITAL_ID = "001";

    /** 会话编号前缀 */
    public static final String SESSION_SN_PREFIX = "session_";

    /** 草稿ID前缀 */
    public static final String DRAFT_ID_PREFIX = "draft_";
}
