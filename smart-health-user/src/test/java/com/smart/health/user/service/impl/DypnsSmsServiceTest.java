package com.smart.health.user.service.impl;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.smart.health.user.config.DypnsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DypnsSmsService 号码认证短信服务单元测试")
class DypnsSmsServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private Client dypnsClient;

    private DypnsSmsService dypnsSmsService;

    @BeforeEach
    void setUp() {
        DypnsProperties props = new DypnsProperties();
        props.setTemplateCode("SMS_123456789");
        dypnsSmsService = new DypnsSmsService(redis, dypnsClient, props);
    }

    @Test
    @DisplayName("deliver 阿里云返回OK - 正常完成不抛异常")
    void deliver_aliyunOk_completesNormally() throws Exception {
        SendSmsVerifyCodeResponseBody body = new SendSmsVerifyCodeResponseBody()
                .setCode("OK")
                .setMessage("OK");
        SendSmsVerifyCodeResponse response = new SendSmsVerifyCodeResponse().setBody(body);
        when(dypnsClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class))).thenReturn(response);

        assertThatCode(() -> {
            dypnsSmsService.deliver("13800138000", "123456");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deliver 阿里云返回非OK - 抛RuntimeException")
    void deliver_aliyunError_throwsRuntimeException() throws Exception {
        SendSmsVerifyCodeResponseBody body = new SendSmsVerifyCodeResponseBody()
                .setCode("isv.BUSINESS_LIMIT_CONTROL")
                .setMessage("业务限流");
        SendSmsVerifyCodeResponse response = new SendSmsVerifyCodeResponse().setBody(body);
        when(dypnsClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> {
            dypnsSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("号码认证短信发送失败")
                .hasMessageContaining("业务限流");
    }

    @Test
    @DisplayName("deliver 网络异常 - 抛RuntimeException")
    void deliver_networkException_throwsRuntimeException() throws Exception {
        when(dypnsClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class)))
                .thenThrow(new RuntimeException("Connection timed out"));

        assertThatThrownBy(() -> {
            dypnsSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection timed out");
    }

    @Test
    @DisplayName("deliver 响应体为空 - 抛RuntimeException")
    void deliver_nullBody_throwsRuntimeException() throws Exception {
        SendSmsVerifyCodeResponse response = new SendSmsVerifyCodeResponse().setBody(null);
        when(dypnsClient.sendSmsVerifyCode(any(SendSmsVerifyCodeRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> {
            dypnsSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("响应体为空");
    }
}
