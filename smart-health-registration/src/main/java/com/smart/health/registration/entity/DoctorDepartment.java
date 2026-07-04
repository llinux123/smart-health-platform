package com.smart.health.registration.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 医生-科室多对多关联实体
 */
@Data
public class DoctorDepartment {

    private Long id;

    /** 医生ID */
    private Long doctorId;

    /** 科室ID */
    private Long departmentId;

    /** 是否主科室 */
    private Boolean isPrimary;

    /** 创建时间 */
    private LocalDateTime createTime;
}
