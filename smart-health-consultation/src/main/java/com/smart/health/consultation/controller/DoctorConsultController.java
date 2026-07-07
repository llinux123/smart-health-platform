package com.smart.health.consultation.controller;

import com.smart.health.common.result.PageResult;
import com.smart.health.common.result.Result;
import com.smart.health.consultation.dto.*;
import com.smart.health.consultation.service.DoctorConsultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 医生端问诊控制器 — 接诊、查看、回复
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/doctor/consultations")
@RequiredArgsConstructor
@Tag(name = "医生问诊", description = "医生接诊、查看详情、回复患者")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorConsultController {

    private final DoctorConsultService doctorConsultService;

    @GetMapping("/pending")
    @Operation(summary = "待接诊列表", description = "分页查询待接诊(PENDING_DOCTOR)和沟通中(DOCTOR_ACTIVE)的问诊会话")
    public Result<PageResult<DoctorConsultSessionVO>> listPending(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {

        DoctorConsultListRequest request = DoctorConsultListRequest.builder()
                .keyword(keyword).page(page).size(size).build();
        PageResult<DoctorConsultSessionVO> result = doctorConsultService.listPending(request);
        return Result.ok(result);
    }

    @GetMapping("/{sessionSn}")
    @Operation(summary = "问诊详情", description = "查看患者信息、AI分析报告、对话历史")
    public Result<DoctorConsultDetailVO> getDetail(@PathVariable String sessionSn) {
        DoctorConsultDetailVO result = doctorConsultService.getDetail(sessionSn);
        return Result.ok(result);
    }

    @PostMapping("/{sessionSn}/reply")
    @Operation(summary = "回复患者", description = "医生回复问诊，可选标记为已解决")
    public Result<DoctorConsultReplyVO> reply(
            @PathVariable String sessionSn,
            @Valid @RequestBody DoctorConsultReplyRequest request) {
        DoctorConsultReplyVO result = doctorConsultService.reply(sessionSn, request);
        return Result.ok(result);
    }

    @PostMapping("/{sessionSn}/resolve")
    @Operation(summary = "标记已解决")
    public Result<Void> resolve(@PathVariable String sessionSn) {
        doctorConsultService.resolve(sessionSn);
        return Result.ok();
    }
}
