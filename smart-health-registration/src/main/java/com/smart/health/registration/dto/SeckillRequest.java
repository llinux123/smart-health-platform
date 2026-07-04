package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒杀抢号请求
 * 注意：patientId 不在此 DTO 中，由 Controller 层从 SecurityContext 获取后独立传入
 */
@Data
@Schema(description = "秒杀抢号请求")
public class SeckillRequest {

    @NotNull(message = "排班ID不能为空")
    @Schema(description = "排班ID", example = "1")
    private Long scheduleId;
}
