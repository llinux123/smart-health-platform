package com.smart.health.user.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.smart.health.user.config.AliyunSmsProperties;
import com.smart.health.user.service.AbstractRedisSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 阿里云短信验证码服务
 * <p>
 * 生产环境实现：通过阿里云 SendSms API 发送验证码。
 * 仅当 sms.provider=aliyun 时激活。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
public class AliyunSmsService extends AbstractRedisSmsService {

    private final Client aliyunSmsClient;
    private final String signName;
    private final String templateCode;

    public AliyunSmsService(StringRedisTemplate redis,
                            Client aliyunSmsClient,
                            AliyunSmsProperties props) {
        super(redis);
        this.aliyunSmsClient = aliyunSmsClient;
        this.signName = props.getSignName();
        this.templateCode = props.getTemplateCode();
    }

    @Override
    protected void deliver(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = aliyunSmsClient.sendSms(request);
            SendSmsResponseBody body = response.getBody();

            if (body == null || !"OK".equals(body.getCode())) {
                String errMsg = body != null ? body.getMessage() : "响应体为空";
                throw new RuntimeException("阿里云短信发送失败: " + errMsg);
            }

            log.info("短信发送成功, phone={}", phone);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("阿里云短信API调用异常", e);
        }
    }
}
