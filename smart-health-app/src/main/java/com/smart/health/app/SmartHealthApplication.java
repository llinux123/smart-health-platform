package com.smart.health.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.smart.health")
@EnableScheduling
@MapperScan(basePackages = {
    "com.smart.health.user.mapper",
    "com.smart.health.registration.mapper",
    "com.smart.health.consultation.mapper",
    "com.smart.health.prescription.mapper"
})
public class SmartHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHealthApplication.class, args);
    }
}
