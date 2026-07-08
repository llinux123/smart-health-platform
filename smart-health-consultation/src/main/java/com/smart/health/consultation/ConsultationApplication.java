package com.smart.health.consultation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.smart.health")
@EnableScheduling
public class ConsultationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultationApplication.class, args);
    }
}
