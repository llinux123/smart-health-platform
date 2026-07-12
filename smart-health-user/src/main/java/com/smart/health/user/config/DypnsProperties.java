package com.smart.health.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 阿里云号码认证服务（Dypnsapi）配置属性。
 * <p>
 * 仅当 sms.provider=dypnsapi 时激活，启动时校验必填字段（fail-fast）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms.dypnsapi")
@ConditionalOnProperty(name = "sms.provider", havingValue = "dypnsapi")
@Validated
public class DypnsProperties {

    @NotBlank(message = "sms.dypnsapi.access-key-id 不能为空（当 sms.provider=dypnsapi 时）")
    private String accessKeyId;

    @NotBlank(message = "sms.dypnsapi.access-key-secret 不能为空")
    private String accessKeySecret;

    @NotBlank(message = "sms.dypnsapi.template-code 不能为空")
    private String templateCode;

    private String endpoint = "dypnsapi.aliyuncs.com";

    private String signName = "";
}
