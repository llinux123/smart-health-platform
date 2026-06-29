package com.smart.health.prescription.entity;

import lombok.Data;

/**
 * 处方明细实体
 * 对应表: t_prescription_item
 */
@Data
public class PrescriptionItem {

    /** 明细ID */
    private Long id;

    /** 处方ID */
    private Long prescriptionId;

    /** 药品ID */
    private Long medicineId;

    /** 药品名称 */
    private String medicineName;

    /** 院区药房ID */
    private Long pharmacyId;

    /** 数量 */
    private Integer quantity;

    /** 单位 */
    private String unit;
}
