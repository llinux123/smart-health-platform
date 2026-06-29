package com.smart.health.consultation.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件上传配置
 */
@Slf4j
@Getter
@Configuration
public class FileUploadConfig {

    @Value("${file.upload-path:/tmp/smart-health/uploads/}")
    private String uploadPath;

    /**
     * 启动时确保上传目录存在
     */
    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建文件上传目录: {}", uploadPath);
            }
        } catch (IOException e) {
            log.error("创建文件上传目录失败: {}", uploadPath, e);
            throw new RuntimeException("无法创建文件上传目录", e);
        }
    }
}
