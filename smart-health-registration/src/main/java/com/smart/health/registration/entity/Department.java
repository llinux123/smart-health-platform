package com.smart.health.registration.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科室信息实体
 */
@Data
public class Department {

    /** 科室ID */
    private Long id;

    /** 科室名称 */
    private String name;

    /** 科室简述 */
    private String description;

    /** 科室图标URL */
    private String icon;

    /** 科室详细介绍 */
    private String intro;

    /** 排序权重 */
    private Integer sortOrder;

    /** 是否启用 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createTime;
}
