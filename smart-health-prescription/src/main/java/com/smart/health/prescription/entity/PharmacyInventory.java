package com.smart.health.prescription.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药房库存实体
 * 对应表: t_pharmacy_inventory
 */
@Data
public class PharmacyInventory {

    /** 库存记录ID */
    private Long id;

    /** 院区药房ID */
    private Long pharmacyId;

    /** 药品ID */
    private Long medicineId;

    /** 药品通用名 */
    private String medicineName;

    /** 实际库存量 */
    private Integer stock;

    /** 冻结库存量 */
    private Integer lockStock;

    /** 单位 */
    private String unit;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
