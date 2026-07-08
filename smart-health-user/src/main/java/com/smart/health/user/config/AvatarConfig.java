package com.smart.health.user.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 头像文件配置 — 存储目录创建 + 静态资源映射
 */
@Slf4j
@Getter
@Configuration
public class AvatarConfig implements WebMvcConfigurer {

    @Value("${avatar.upload-path:./avatars/}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建头像存储目录: {}", uploadPath);
            }
        } catch (IOException e) {
            log.error("创建头像存储目录失败: {}", uploadPath, e);
            throw new RuntimeException("无法创建头像存储目录", e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/auth/avatars/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
