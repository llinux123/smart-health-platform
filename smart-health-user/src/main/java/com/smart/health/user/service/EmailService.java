package com.smart.health.user.service;

public interface EmailService {

    String sendCode(String email);

    boolean verifyCode(String email, String code);
}
