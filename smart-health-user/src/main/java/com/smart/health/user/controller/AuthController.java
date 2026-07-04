package com.smart.health.user.controller;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.Result;
import com.smart.health.common.result.ResultCode;
import com.smart.health.user.dto.LoginRequest;
import com.smart.health.user.dto.LoginResponse;
import com.smart.health.user.dto.ProfileResponse;
import com.smart.health.user.dto.RegisterRequest;
import com.smart.health.user.service.PatientAuthService;
import com.smart.health.user.service.StaffAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统一认证控制器（患者 + 员工）
 */
@Tag(name = "认证", description = "注册/登录接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PatientAuthService patientAuthService;
    private final StaffAuthService staffAuthService;

    @Operation(summary = "患者注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        patientAuthService.register(request);
        return Result.ok();
    }

    @Operation(summary = "统一登录（患者/员工）")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = switch (request.getLoginType()) {
            case "PATIENT" -> patientAuthService.login(request);
            case "STAFF" -> staffAuthService.login(request.getUsername(), request.getPassword());
            default -> throw new BusinessException(ResultCode.INVALID_LOGIN_TYPE);
        };
        return Result.ok("登录成功", response);
    }

    @Operation(summary = "获取当前患者信息")
    @GetMapping("/profile")
    public Result<ProfileResponse> getProfile() {
        return Result.ok(patientAuthService.getProfile());
    }
}
