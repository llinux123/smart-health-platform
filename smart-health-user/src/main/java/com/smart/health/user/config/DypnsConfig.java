package com.smart.health.user.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云号码认证 Client Bean 工厂。
 * 仅当 sms.provider=dypnsapi 时激活。
 */
@Configuration
@ConditionalOnProperty(name = "sms.provider", havingValue = "dypnsapi")
public class DypnsConfig {

    @Bean
    public Client dypnsClient(DypnsProperties props) throws Exception {
        Config config = new Config()
                .setAccessKeyId(props.getAccessKeyId())
                .setAccessKeySecret(props.getAccessKeySecret());
        config.endpoint = props.getEndpoint();
        return new Client(config);
    }
}
