package com.smart.health.registration.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生信息实体
 */
@Data
public class Doctor {

    /** 医生ID */
    private Long id;

    /** 医生姓名 */
    private String name;

    /** 职称 */
    private String title;

    /** 头像URL */
    private String avatar;

    /** 所属科室 */
    private String deptName;

    /** 主科室ID（非数据库字段，通过 JOIN t_doctor_department 填充） */
    private Long primaryDepartmentId;

    /** 擅长领域 */
    private String specialty;

    /** 医生简介 */
    private String intro;

    /** 创建时间 */
    private LocalDateTime createTime;
}
