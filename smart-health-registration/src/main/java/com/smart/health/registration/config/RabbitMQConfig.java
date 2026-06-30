package com.smart.health.registration.config;

import com.smart.health.common.constant.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 定义挂号相关交换机、队列、绑定关系及死信队列
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
     * 挂号订单队列（绑定死信交换机）
     */
    @Bean
    public Queue registrationOrderQueue() {
        return QueueBuilder.durable(CommonConstants.MQ_QUEUE_REGISTRATION_ORDER)
                .withArgument("x-dead-letter-exchange", CommonConstants.MQ_DLX_REGISTRATION)
                .withArgument("x-dead-letter-routing-key", CommonConstants.MQ_ROUTING_KEY_DLQ)
                .build();
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

    /**
     * 死信交换机
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(CommonConstants.MQ_DLX_REGISTRATION, true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(CommonConstants.MQ_DLQ_REGISTRATION)
                .withArgument("x-message-ttl", CommonConstants.DLQ_MESSAGE_TTL_MS)
                .build();
    }

    /**
     * 死信队列绑定到死信交换机
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(CommonConstants.MQ_ROUTING_KEY_DLQ);
    }
}
