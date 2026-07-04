package com.smart.health.registration.controller;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.Result;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.ScheduleVO;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.service.RegistrationOrderService;
import com.smart.health.registration.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班管理控制器
 */
@Tag(name = "排班管理", description = "医生排班查询、创建及秒杀抢号")
@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final RegistrationOrderService registrationOrderService;

    @Operation(summary = "查看可预约的医生排班列表")
    @GetMapping("/api/v1/schedule/list")
    public Result<List<ScheduleVO>> list(
            @Parameter(description = "科室名称") @RequestParam(required = false) String deptName,
            @Parameter(description = "科室ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "出诊日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        List<ScheduleVO> list = scheduleService.getAvailableSchedules(deptName, departmentId, workDate);
        return Result.ok(list);
    }

    @Operation(summary = "获取排班详情（含医生头像、科室信息）")
    @GetMapping("/api/v1/schedule/detail/{id}")
    public Result<ScheduleVO> detail(@PathVariable Long id) {
        return Result.ok(scheduleService.getScheduleDetail(id));
    }

    @Operation(summary = "运营人员创建排班")
    @PostMapping("/api/v1/schedule/create")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody ScheduleCreateRequest request) {
        scheduleService.createSchedule(request);
        return Result.ok();
    }

    @Operation(summary = "秒杀抢号")
    @PostMapping("/api/v1/registration/seckill")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<SeckillResponse> seckill(@Valid @RequestBody SeckillRequest request) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        SeckillResponse response = scheduleService.seckill(request, patientId);
        return Result.ok(response);
    }

    @Operation(summary = "根据订单号查询订单详情（含排班及医生信息）")
    @GetMapping("/api/v1/registration/order/detail")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN','DOCTOR')")
    public Result<OrderVO> getOrderDetail(
            @Parameter(description = "订单号") @RequestParam String orderSn) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        String role = SecurityUtils.getCurrentRole();
        OrderVO order = registrationOrderService.getOrderVOByOrderSn(orderSn, patientId, role);
        return Result.ok(order);
    }

    @Operation(summary = "查询当前患者的挂号订单列表（含排班及医生信息）")
    @GetMapping("/api/v1/registration/order/list")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<List<OrderVO>> listOrders() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        List<OrderVO> orders = registrationOrderService.listOrderVOByPatientId(patientId);
        return Result.ok(orders);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/api/v1/registration/order/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> cancelOrder(
            @Parameter(description = "订单号") @RequestParam String orderSn) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        registrationOrderService.cancelOrder(orderSn, patientId);
        return Result.ok();
    }

    @Operation(summary = "支付订单")
    @PostMapping("/api/v1/registration/order/pay")
    @PreAuthorize("hasRole('PATIENT')")
    public Result<Void> payOrder(
            @Parameter(description = "订单号") @RequestParam String orderSn) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        registrationOrderService.payOrder(orderSn, patientId);
        return Result.ok();
    }
}
