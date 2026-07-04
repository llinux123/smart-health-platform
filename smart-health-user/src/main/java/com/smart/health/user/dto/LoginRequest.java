package com.smart.health.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录类型: PATIENT（患者） / STAFF（员工）
     */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;
}
