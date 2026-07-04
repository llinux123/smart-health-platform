package com.smart.health.prescription.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品字典实体
 */
@Data
public class Medicine {

    /** 药品ID */
    private Long id;

    /** 药品通用名 */
    private String name;

    /** 商品名 */
    private String brandName;

    /** 药品分类 */
    private String category;

    /** 常用规格 */
    private String spec;

    /** 最小单位 */
    private String unit;

    /** 生产厂家 */
    private String manufacturer;

    /** 国药准字 */
    private String approvalNumber;

    /** 参考单价 */
    private BigDecimal price;

    /** 是否OTC */
    private Boolean isOtc;

    /** 是否启用 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
