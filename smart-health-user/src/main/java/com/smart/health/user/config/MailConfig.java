package com.smart.health.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "email.provider", havingValue = "smtp")
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(Environment env) {
        String host = env.getProperty("spring.mail.host", env.getProperty("SMTP_HOST", ""));
        int port = Integer.parseInt(env.getProperty("spring.mail.port", env.getProperty("SMTP_PORT", "587")));
        String username = env.getProperty("spring.mail.username", env.getProperty("SMTP_USERNAME", ""));
        String password = env.getProperty("spring.mail.password", env.getProperty("SMTP_PASSWORD", ""));

        log.info("Configuring JavaMailSender: host={}, port={}, username={}", host, port, username);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }
}
