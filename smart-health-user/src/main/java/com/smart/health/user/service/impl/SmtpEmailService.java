package com.smart.health.user.service.impl;

import com.smart.health.user.service.AbstractRedisEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp")
public class SmtpEmailService extends AbstractRedisEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailService(StringRedisTemplate redis,
                            JavaMailSender mailSender,
                            Environment env) {
        super(redis);
        this.mailSender = mailSender;
        this.fromAddress = env.getProperty("spring.mail.username", env.getProperty("SMTP_USERNAME", ""));
        log.info("SmtpEmailService fromAddress resolved to: [{}]", this.fromAddress);
    }

    @Override
    protected void deliver(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("智慧医疗平台 - 邮箱验证码");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
                        <h2 style="color: #333; margin-bottom: 16px;">邮箱验证码</h2>
                        <p style="color: #666; line-height: 1.6;">您好，您正在绑定智慧医疗平台的邮箱，验证码为：</p>
                        <div style="background: #f5f7fa; border-radius: 8px; padding: 20px; text-align: center; margin: 16px 0;">
                            <span style="font-size: 32px; font-weight: bold; color: #2563EB; letter-spacing: 8px;">%s</span>
                        </div>
                        <p style="color: #999; font-size: 13px;">验证码有效期为 5 分钟，请勿泄露给他人。</p>
                        <p style="color: #999; font-size: 13px;">如非本人操作，请忽略此邮件。</p>
                    </div>
                    """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("邮箱验证码发送成功, email={}, code={}", email, code);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
