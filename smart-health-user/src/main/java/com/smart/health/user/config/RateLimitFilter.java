package com.smart.health.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.result.Result;
import com.smart.health.common.result.ResultCode;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis 的认证接口限流过滤器
 * <p>
 * 对 /api/v1/auth/ 下的敏感接口（登录、发验证码等）实施每秒限流，
 * 防止暴力破解和短信/邮件轰炸。
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String RATE_LIMIT_PREFIX = "ratelimit:auth:";

    private static final Map<String, RateLimitConfig> PATH_CONFIGS = new ConcurrentHashMap<>();

    static {
        PATH_CONFIGS.put("/api/v1/auth/send-code",
                new RateLimitConfig(1, Duration.ofSeconds(60), "短信验证码发送太频繁，请1分钟后再试"));
        PATH_CONFIGS.put("/api/v1/auth/send-email-code",
                new RateLimitConfig(1, Duration.ofSeconds(60), "邮箱验证码发送太频繁，请1分钟后再试"));
        PATH_CONFIGS.put("/api/v1/auth/login",
                new RateLimitConfig(5, Duration.ofSeconds(60), "登录尝试过于频繁，请1分钟后再试"));
        PATH_CONFIGS.put("/api/v1/auth/login/sms",
                new RateLimitConfig(5, Duration.ofSeconds(60), "登录尝试过于频繁，请1分钟后再试"));
        PATH_CONFIGS.put("/api/v1/auth/reset-password",
                new RateLimitConfig(3, Duration.ofSeconds(60), "密码重置尝试过于频繁，请1分钟后再试"));
    }

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitConfig config = PATH_CONFIGS.get(path);

        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = RATE_LIMIT_PREFIX + path + ":" + clientIp;

        Long currentCount = redisTemplate.opsForValue().increment(key);
        if (currentCount == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (currentCount == 1) {
            redisTemplate.expire(key, config.window);
        }

        if (currentCount > config.maxRequests) {
            log.warn("Rate limit exceeded for path={}, ip={}", path, clientIp);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(429);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.fail(429, config.message)));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private record RateLimitConfig(int maxRequests, Duration window, String message) {}
}
