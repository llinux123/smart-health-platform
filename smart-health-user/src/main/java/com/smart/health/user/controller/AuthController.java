package com.smart.health.user.controller;

import com.smart.health.common.result.Result;
import com.smart.health.user.dto.LoginRequest;
import com.smart.health.user.dto.LoginResponse;
import com.smart.health.user.dto.ProfileResponse;
import com.smart.health.user.dto.RegisterRequest;
import com.smart.health.user.service.PatientAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 患者认证控制器
 */
@Tag(name = "患者认证", description = "注册/登录接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PatientAuthService patientAuthService;

    @Operation(summary = "患者注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        patientAuthService.register(request);
        return Result.ok();
    }

    @Operation(summary = "患者登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = patientAuthService.login(request);
        return Result.ok("登录成功", response);
    }

    @Operation(summary = "获取当前患者信息")
    @GetMapping("/profile")
    public Result<ProfileResponse> getProfile() {
        return Result.ok(patientAuthService.getProfile());
    }
}
