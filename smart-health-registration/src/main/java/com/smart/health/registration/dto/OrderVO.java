package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 挂号订单视图对象（包含排班及医生展示信息）
 */
@Data
@Schema(description = "挂号订单视图")
public class OrderVO {

    @Schema(description = "订单流水号")
    private String orderSn;

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "排班ID")
    private Long scheduleId;

    @Schema(description = "科室名称")
    private String deptName;

    @Schema(description = "医生姓名")
    private String doctorName;

    @Schema(description = "出诊日期")
    private LocalDate workDate;

    @Schema(description = "班次 (1:上午 2:下午)")
    private Integer shift;

    @Schema(description = "班次名称")
    private String shiftName;

    @Schema(description = "挂号费")
    private BigDecimal fee;

    @Schema(description = "状态 (0:排队中 1:待支付 2:已支付 3:已就诊 4:已退号)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;
}
