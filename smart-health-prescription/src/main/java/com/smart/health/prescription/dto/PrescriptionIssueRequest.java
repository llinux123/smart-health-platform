package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 开具处方请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "开具处方请求")
public class PrescriptionIssueRequest {

    @NotNull(message = "患者ID不能为空")
    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "关联问诊ID（可选）")
    private Long consultationId;

    @NotBlank(message = "诊断结论不能为空")
    @Schema(description = "临床诊断结论")
    private String diagnosis;

    @NotEmpty(message = "药品列表不能为空")
    @Schema(description = "药品明细列表")
    private List<MedicineItem> medicines;

    /**
     * 药品明细项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "药品明细项")
    public static class MedicineItem {

        @NotNull(message = "药品ID不能为空")
        @Schema(description = "药品ID")
        private Long medicineId;

        @NotBlank(message = "药品名称不能为空")
        @Schema(description = "药品名称")
        private String medicineName;

        @NotNull(message = "药房ID不能为空")
        @Schema(description = "院区药房ID")
        private Long pharmacyId;

        @NotNull(message = "数量不能为空")
        @Schema(description = "数量")
        private Integer quantity;

        @NotBlank(message = "单位不能为空")
        @Schema(description = "单位")
        private String unit;
    }
}
