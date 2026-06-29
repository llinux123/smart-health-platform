package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀抢号请求
 */
@Data
@Schema(description = "秒杀抢号请求")
public class SeckillRequest {

    @NotNull(message = "排班ID不能为空")
    @Schema(description = "排班ID", example = "1")
    private Long scheduleId;

    @NotNull(message = "患者ID不能为空")
    @Schema(description = "患者ID", example = "100")
    private Long patientId;
}
