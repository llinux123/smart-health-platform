package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 排班信息视图对象
 */
@Data
@Schema(description = "排班信息")
public class ScheduleVO {

    @Schema(description = "排班ID")
    private Long id;

    @Schema(description = "医生ID")
    private Long doctorId;

    @Schema(description = "医生姓名")
    private String doctorName;

    @Schema(description = "医生头像URL")
    private String doctorAvatar;

    @Schema(description = "科室ID")
    private Long departmentId;

    @Schema(description = "科室名称")
    private String deptName;

    @Schema(description = "出诊日期")
    private LocalDate workDate;

    @Schema(description = "班次")
    private Integer shift;

    @Schema(description = "班次名称")
    private String shiftName;

    @Schema(description = "总号源量")
    private Integer totalCount;

    @Schema(description = "剩余可抢号源量")
    private Integer visibleCount;

    @Schema(description = "挂号费")
    private BigDecimal price;
}
