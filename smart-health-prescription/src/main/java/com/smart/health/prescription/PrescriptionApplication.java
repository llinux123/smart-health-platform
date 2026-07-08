package com.smart.health.prescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smart.health")
public class PrescriptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrescriptionApplication.class, args);
    }
}
