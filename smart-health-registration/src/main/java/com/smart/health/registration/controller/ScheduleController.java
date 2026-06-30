package com.smart.health.registration.controller;

import com.smart.health.common.result.Result;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.ScheduleVO;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "查看可预约的医生排班列表")
    @GetMapping("/api/v1/schedule/list")
    public Result<List<ScheduleVO>> list(
            @Parameter(description = "科室名称") @RequestParam(required = false) String deptName,
            @Parameter(description = "出诊日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        List<ScheduleVO> list = scheduleService.getAvailableSchedules(deptName, workDate);
        return Result.ok(list);
    }

    @Operation(summary = "运营人员创建排班")
    @PostMapping("/api/v1/schedule/create")
    public Result<Void> create(@Valid @RequestBody ScheduleCreateRequest request) {
        scheduleService.createSchedule(request);
        return Result.ok();
    }

    @Operation(summary = "秒杀抢号")
    @PostMapping("/api/v1/registration/seckill")
    public Result<SeckillResponse> seckill(@Valid @RequestBody SeckillRequest request) {
        SeckillResponse response = scheduleService.seckill(request);
        return Result.ok(response);
    }
}
