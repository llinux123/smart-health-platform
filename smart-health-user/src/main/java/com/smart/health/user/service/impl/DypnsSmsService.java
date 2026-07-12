package com.smart.health.user.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.smart.health.user.config.DypnsProperties;
import com.smart.health.user.service.AbstractRedisSmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 阿里云号码认证短信验证码服务。
 * <p>
 * 生产环境实现：通过 Dypnsapi SendSmsVerifyCode API 发送验证码。
 * 验证码由服务端生成并存入 Redis，发送到手机后由 {@link #verifyCode} 从 Redis 校验。
 * 仅当 sms.provider=dypnsapi 时激活。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "dypnsapi")
public class DypnsSmsService extends AbstractRedisSmsService {

    private final Client dypnsClient;
    private final String templateCode;
    private final String signName;

    public DypnsSmsService(StringRedisTemplate redis,
                           Client dypnsClient,
                           DypnsProperties props) {
        super(redis);
        this.dypnsClient = dypnsClient;
        this.templateCode = props.getTemplateCode();
        this.signName = props.getSignName();
    }

    @Override
    protected void deliver(String phone, String code) {
        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\",\"min\":\"5\"}");

            if (signName != null && !signName.isEmpty()) {
                request.setSignName(signName);
            }

            SendSmsVerifyCodeResponse response = dypnsClient.sendSmsVerifyCode(request);
            SendSmsVerifyCodeResponseBody body = response.getBody();

            if (body == null || !"OK".equals(body.getCode())) {
                String errMsg = body != null ? body.getMessage() : "响应体为空";
                throw new RuntimeException("号码认证短信发送失败: " + errMsg);
            }

            log.info("号码认证短信发送成功, phone={}", phone);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("号码认证短信API调用异常", e);
        }
    }
}
