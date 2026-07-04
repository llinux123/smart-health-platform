package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建排班请求
 */
@Data
@Schema(description = "创建排班请求")
public class ScheduleCreateRequest {

    @NotNull(message = "医生ID不能为空")
    @Schema(description = "医生ID", example = "1001")
    private Long doctorId;

    @Schema(description = "科室ID(优先使用)")
    private Long departmentId;

    @Schema(description = "科室名称(向后兼容)")
    private String deptName;

    @NotNull(message = "出诊日期不能为空")
    @Schema(description = "出诊日期", example = "2026-07-01")
    private LocalDate workDate;

    @NotNull(message = "班次不能为空")
    @Schema(description = "班次 (1:上午 2:下午)", example = "1")
    private Integer shift;

    @NotNull(message = "总号源量不能为空")
    @Min(value = 1, message = "总号源量至少为1")
    @Schema(description = "总号源量", example = "30")
    private Integer totalCount;

    @NotNull(message = "挂号费不能为空")
    @DecimalMin(value = "0.00", message = "挂号费不能为负数")
    @Schema(description = "挂号费", example = "50.00")
    private BigDecimal price;
}
