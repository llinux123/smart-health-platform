package com.smart.health.user.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.smart.health.user.config.AliyunSmsProperties;
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
@DisplayName("AliyunSmsService 阿里云短信服务单元测试")
class AliyunSmsServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private Client aliyunSmsClient;

    private AliyunSmsService aliyunSmsService;

    @BeforeEach
    void setUp() {
        AliyunSmsProperties props = new AliyunSmsProperties();
        props.setSignName("智慧医疗");
        props.setTemplateCode("SMS_123456789");
        aliyunSmsService = new AliyunSmsService(redis, aliyunSmsClient, props);
    }

    @Test
    @DisplayName("deliver 阿里云返回OK - 正常完成不抛异常")
    void deliver_aliyunOk_completesNormally() throws Exception {
        // Given
        SendSmsResponseBody body = new SendSmsResponseBody()
                .setCode("OK")
                .setMessage("OK");
        SendSmsResponse response = new SendSmsResponse().setBody(body);
        when(aliyunSmsClient.sendSms(any(SendSmsRequest.class))).thenReturn(response);

        // When & Then
        assertThatCode(() -> {
            aliyunSmsService.deliver("13800138000", "123456");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deliver 阿里云返回非OK - 抛RuntimeException")
    void deliver_aliyunError_throwsRuntimeException() throws Exception {
        // Given
        SendSmsResponseBody body = new SendSmsResponseBody()
                .setCode("isv.BUSINESS_LIMIT_CONTROL")
                .setMessage("业务限流");
        SendSmsResponse response = new SendSmsResponse().setBody(body);
        when(aliyunSmsClient.sendSms(any(SendSmsRequest.class))).thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> {
            aliyunSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("阿里云短信发送失败")
                .hasMessageContaining("业务限流");
    }

    @Test
    @DisplayName("deliver 网络异常 - 抛RuntimeException")
    void deliver_networkException_throwsRuntimeException() throws Exception {
        // Given
        when(aliyunSmsClient.sendSms(any(SendSmsRequest.class)))
                .thenThrow(new RuntimeException("Connection timed out"));

        // When & Then
        assertThatThrownBy(() -> {
            aliyunSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection timed out");
    }

    @Test
    @DisplayName("deliver 响应体为空 - 抛RuntimeException")
    void deliver_nullBody_throwsRuntimeException() throws Exception {
        // Given
        SendSmsResponse response = new SendSmsResponse().setBody(null);
        when(aliyunSmsClient.sendSms(any(SendSmsRequest.class))).thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> {
            aliyunSmsService.deliver("13800138000", "123456");
        }).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("响应体为空");
    }
}
