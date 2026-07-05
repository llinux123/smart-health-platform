package com.smart.health.user.controller;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.Result;
import com.smart.health.common.result.ResultCode;
import com.smart.health.user.dto.*;
import com.smart.health.user.service.PatientAuthService;
import com.smart.health.user.service.StaffAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 统一认证控制器（患者 + 员工）
 */
@Tag(name = "认证", description = "注册/登录/登出接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PatientAuthService patientAuthService;
    private final StaffAuthService staffAuthService;

    @Operation(summary = "患者注册")
    @PostMapping("/register")
    @Deprecated
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        patientAuthService.register(request);
        return Result.ok();
    }

    @Operation(summary = "发送短信验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SmsSendRequest request) {
        patientAuthService.sendSmsCode(request.getPhone());
        return Result.ok("验证码已发送", null);
    }

    @Operation(summary = "短信验证码登录（登录注册合一）")
    @PostMapping("/login/sms")
    public Result<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        LoginResponse response = patientAuthService.smsLogin(request);
        String msg = Boolean.TRUE.equals(response.getIsNewUser()) ? "注册成功" : "登录成功";
        return Result.ok(msg, response);
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

    @Operation(summary = "登出", description = "客户端主动登出（无状态 JWT 模式，服务端不做 token 失效）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info("用户登出");
        return Result.ok("登出成功", null);
    }

    @Operation(summary = "身份绑定", description = "患者绑定实名信息（真实姓名、身份证、性别、邮箱）")
    @PostMapping("/bind-identity")
    public Result<ProfileResponse> bindIdentity(@Valid @RequestBody BindIdentityRequest request) {
        return Result.ok("身份绑定成功", patientAuthService.bindIdentity(request));
    }

    @Operation(summary = "重置密码", description = "通过短信验证码重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        patientAuthService.resetPassword(request);
        return Result.ok("密码重置成功", null);
    }
}
