package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 药房库存 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "药房库存信息")
public class InventoryVO {

    @Schema(description = "院区药房ID")
    private Long pharmacyId;

    @Schema(description = "药品名称")
    private String medicineName;

    @Schema(description = "实际库存量")
    private Integer stock;

    @Schema(description = "冻结库存量")
    private Integer lockStock;

    @Schema(description = "单位")
    private String unit;
}
