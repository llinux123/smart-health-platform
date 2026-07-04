package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 入库请求
 */
@Data
@Schema(description = "入库请求")
public class InboundRequest {

    @NotNull(message = "药房ID不能为空")
    @Schema(description = "院区药房ID")
    private Long pharmacyId;

    @NotNull(message = "药品ID不能为空")
    @Schema(description = "药品ID")
    private Long medicineId;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量至少为1")
    @Schema(description = "入库数量")
    private Integer quantity;

    @Schema(description = "入库原因")
    private String reason;
}
