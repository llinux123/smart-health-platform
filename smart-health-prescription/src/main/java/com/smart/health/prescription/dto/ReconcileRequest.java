package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 盘点请求
 */
@Data
@Schema(description = "盘点请求")
public class ReconcileRequest {

    @NotNull(message = "药房ID不能为空")
    @Schema(description = "院区药房ID")
    private Long pharmacyId;

    @NotNull(message = "药品ID不能为空")
    @Schema(description = "药品ID")
    private Long medicineId;

    @NotNull(message = "实际库存量不能为空")
    @Min(value = 0, message = "实际库存量不能为负数")
    @Schema(description = "实际盘点库存量")
    private Integer actualStock;

    @Schema(description = "盘点说明")
    private String reason;
}
