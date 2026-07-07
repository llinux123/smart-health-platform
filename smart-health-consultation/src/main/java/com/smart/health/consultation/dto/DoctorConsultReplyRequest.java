package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生回复请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "医生回复请求")
public class DoctorConsultReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Schema(description = "回复内容", required = true)
    private String message;

    @Schema(description = "操作类型：REPLY=仅回复, RESOLVE=回复并标记已解决", defaultValue = "REPLY")
    private String action = "REPLY";
}
