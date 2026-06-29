package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀抢号响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "秒杀抢号响应")
public class SeckillResponse {

    @Schema(description = "挂号订单号")
    private String orderSn;

    @Schema(description = "状态: QUEUING-排队中")
    private String status;
}
