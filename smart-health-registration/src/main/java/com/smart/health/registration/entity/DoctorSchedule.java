package com.smart.health.registration.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医生排班号源实体
 */
@Data
public class DoctorSchedule {

    /** 排班ID */
    private Long id;

    /** 医生ID */
    private Long doctorId;

    /** 医生姓名（非数据库字段，通过JOIN查询填充） */
    private String doctorName;

    /** 医生头像（非数据库字段，通过JOIN查询填充） */
    private String doctorAvatar;

    /** 科室ID */
    private Long departmentId;

    /** 科室名称 */
    private String deptName;

    /** 出诊日期 */
    private LocalDate workDate;

    /** 班次 (1:上午 2:下午) */
    private Integer shift;

    /** 总号源量 */
    private Integer totalCount;

    /** 剩余可抢号源量 */
    private Integer visibleCount;

    /** 挂号费 */
    private BigDecimal price;

    /** 乐观锁版本号 */
    private Integer version;
}
