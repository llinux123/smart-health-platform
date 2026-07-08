package com.smart.health.consultation.dto;

import lombok.Data;

@Data
public class PatientInfo {
    private Long id;
    private String name;
    private Integer gender;
    private java.sql.Date birthday;
}
