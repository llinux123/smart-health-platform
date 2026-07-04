package com.smart.health.consultation.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * 会话 API ETag 缓存配置
 * 为问诊会话列表及详情接口启用 Shallow ETag 过滤，
 * 前端带 If-None-Match 请求时将返回 304 Not Modified。
 */
@Configuration
public class WebEtagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        // 只对问诊会话相关接口启用 ETag
        registration.addUrlPatterns("/api/v1/ai/sessions/*");
        return registration;
    }
}
