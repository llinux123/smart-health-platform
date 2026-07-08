package com.smart.health.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;

    /**
     * 关联医生ID（仅医生角色有值）
     */
    private Long doctorId;

    /**
     * 兼容旧版患者登录（患者角色有值）
     */
    private Long patientId;

    /**
     * 是否新注册用户（短信验证码登录首次创建账号）
     */
    private Boolean isNewUser;

    /**
     * 是否要求重置密码（新注册用户首次登录后应修改密码）
     */
    @Builder.Default
    private Boolean requirePasswordReset = false;
}
