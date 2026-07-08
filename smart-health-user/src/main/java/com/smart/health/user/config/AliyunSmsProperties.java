package com.smart.health.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云短信服务配置属性。
 * <p>
 * 仅当 sms.provider=aliyun 时激活，启动时校验必填字段（fail-fast）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.aliyun")
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
@Validated
public class AliyunSmsProperties {

    @NotBlank(message = "sms.aliyun.access-key-id 不能为空（当 sms.provider=aliyun 时）")
    private String accessKeyId;

    @NotBlank(message = "sms.aliyun.access-key-secret 不能为空")
    private String accessKeySecret;

    @NotBlank(message = "sms.aliyun.sign-name 不能为空")
    private String signName;

    @NotBlank(message = "sms.aliyun.template-code 不能为空")
    private String templateCode;
}
