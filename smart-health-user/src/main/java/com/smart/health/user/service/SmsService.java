package com.smart.health.user.service;

/**
 * 短信验证码服务接口
 */
public interface SmsService {

    /**
     * 发送验证码
     * @param phone 手机号
     * @return 生成的验证码（仅Mock模式返回，生产环境返回null）
     */
    String sendCode(String phone);

    /**
     * 校验验证码（一次性消费，校验通过后删除）
     * @param phone 手机号
     * @param code  验证码
     * @return true=校验通过
     */
    boolean verifyCode(String phone, String code);
}
