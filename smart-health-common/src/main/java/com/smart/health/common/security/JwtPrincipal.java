package com.smart.health.common.security;

import lombok.Getter;

/**
 * 通用 JWT 认证主体 — 从 Token claims 中提取的用户信息。
 * <p>
 * 用于非 user-service 的微服务，避免依赖数据库查询 UserDetails。
 * 各服务通过 {@link SecurityUtils} 获取当前用户信息时，
 * 底层自动适配此类型。
 */
@Getter
public class JwtPrincipal {

    private final Long userId;
    private final String role;
    private final String username;
    private final Long doctorId;

    public JwtPrincipal(Long userId, String role, String username, Long doctorId) {
        this.userId = userId;
        this.role = role;
        this.username = username;
        this.doctorId = doctorId;
    }
}
