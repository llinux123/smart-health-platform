package com.smart.health.prescription.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处方审核请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "处方审核请求")
public class PrescriptionAuditRequest {

    @NotBlank(message = "审核动作不能为空")
    @Schema(description = "审核动作: APPROVE-通过, REJECT-驳回")
    private String action;

    @Schema(description = "审核意见/备注")
    private String comments;
}
