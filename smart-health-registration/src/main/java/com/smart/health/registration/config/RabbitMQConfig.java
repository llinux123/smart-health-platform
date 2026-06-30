package com.smart.health.registration.config;

import com.smart.health.common.constant.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 定义挂号相关交换机、队列和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 挂号交换机（Topic 类型）
     */
    @Bean
    public TopicExchange registrationExchange() {
        return new TopicExchange(CommonConstants.MQ_EXCHANGE_REGISTRATION, true, false);
    }

    /**
     * 挂号订单队列
     */
    @Bean
    public Queue registrationOrderQueue() {
        return new Queue(CommonConstants.MQ_QUEUE_REGISTRATION_ORDER, true, false, false);
    }

    /**
     * 队列绑定到交换机
     */
    @Bean
    public Binding registrationOrderBinding(Queue registrationOrderQueue, TopicExchange registrationExchange) {
        return BindingBuilder.bind(registrationOrderQueue)
                .to(registrationExchange)
                .with(CommonConstants.MQ_ROUTING_KEY_ORDER_CREATE);
    }
}
