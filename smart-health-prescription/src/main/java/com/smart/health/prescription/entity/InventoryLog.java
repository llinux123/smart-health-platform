package com.smart.health.prescription.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存变动日志实体
 */
@Data
public class InventoryLog {

    /** 日志ID */
    private Long id;

    /** 药房ID */
    private Long pharmacyId;

    /** 药品ID */
    private Long medicineId;

    /** 变动类型: INBOUND/OUTBOUND/RECONCILE/DEDUCT/RESTORE */
    private String changeType;

    /** 变动数量(正=增,负=减) */
    private Integer quantityChange;

    /** 变动前库存 */
    private Integer stockBefore;

    /** 变动后库存 */
    private Integer stockAfter;

    /** 变动原因 */
    private String reason;

    /** 操作人ID */
    private Long operatorId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
