package com.smart.health.user.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * Token claims 包含角色信息，支持患者和员工双体系鉴权
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token（患者）
     */
    public String generatePatientToken(Long patientId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", patientId);
        claims.put("username", username);
        claims.put("role", "PATIENT");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成JWT Token（员工）
     *
     * @param staffId  员工ID
     * @param username 用户名
     * @param role     角色: DOCTOR / PHARMACIST / ADMIN
     * @param doctorId 关联医生ID（仅 DOCTOR 角色，其他传 null）
     */
    public String generateStaffToken(Long staffId, String username, String role, Long doctorId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", staffId);
        claims.put("username", username);
        claims.put("role", role);
        if (doctorId != null) {
            claims.put("doctorId", doctorId);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从Token中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID（通用）
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从Token中获取关联医生ID（仅 DOCTOR 角色）
     */
    public Long getDoctorIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("doctorId", Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 兼容旧版：从Token中获取患者ID（仅当 role=PATIENT 时有效）
     */
    @Deprecated
    public Long getPatientIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String role = claims.get("role", String.class);
        if (!"PATIENT".equals(role)) {
            return null;
        }
        return claims.get("userId", Long.class);
    }
}
