package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评分请求
 */
@Data
@Schema(description = "问诊评分请求")
public class RatingRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    @Schema(description = "评分(1-5星)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rating;

    @Schema(description = "文字反馈(可选)")
    private String feedback;
}
