package com.smart.health.common.security;

import com.smart.health.common.constant.CommonConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * 通用 JWT 认证过滤器（用于非 user-service 的微服务）
 * <p>
 * 仅校验 JWT 签名和过期时间，不访问数据库。
 * 从 Token claims 中提取 userId / role / username 等信息，
 * 构建 {@link JwtPrincipal} 放入 SecurityContext。
 * <p>
 * user-service 通过配置 {@code jwt.auth.filter.enabled=false} 禁用此过滤器，
 * 使用自己基于 DB 的 JwtAuthenticationFilter。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "jwt.auth.filter.enabled", havingValue = "true", matchIfMissing = true)
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = parseClaims(token);
                // userId 和 doctorId 可能不存在或类型不匹配，需要安全获取
                Object userIdObj = claims.get("userId");
                Long userId = userIdObj != null ? ((Number) userIdObj).longValue() : null;
                
                Object doctorIdObj = claims.get("doctorId");
                Long doctorId = doctorIdObj != null ? ((Number) doctorIdObj).longValue() : null;
                
                String role = claims.get("role", String.class);
                String subject = claims.getSubject();
                
                log.debug("JWT Auth: userId={}, role={}, subject={}, doctorId={}", userId, role, subject, doctorId);
                
                JwtPrincipal principal = new JwtPrincipal(userId, role, subject, doctorId);

                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + principal.getRole())
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT Auth: Authentication set successfully");
            } catch (Exception e) {
                log.warn("JWT Auth: Failed to authenticate - {}", e.getMessage());
                // Token 无效或过期，不设置认证信息，后续由 Spring Security 拦截
            }
        }

        filterChain.doFilter(request, response);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
