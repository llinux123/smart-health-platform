package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处方药品明细 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "处方药品明细")
public class PrescriptionItemVO {

    @Schema(description = "药品名称")
    private String medicineName;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "单位")
    private String unit;
}
