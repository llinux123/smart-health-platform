package com.smart.health.user.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云短信 Client Bean 工厂。
 * 仅当 sms.provider=aliyun 时激活。
 */
@Configuration
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
public class AliyunSmsConfig {

    @Bean
    public Client aliyunSmsClient(AliyunSmsProperties props) throws Exception {
        Config config = new Config()
                .setAccessKeyId(props.getAccessKeyId())
                .setAccessKeySecret(props.getAccessKeySecret());
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new Client(config);
    }
}
