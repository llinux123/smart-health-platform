package com.smart.health.common.security;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 * 从 SecurityContextHolder 中提取当前认证患者的信息
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 获取当前认证患者的ID
     *
     * @return 患者ID
     * @throws BusinessException 未认证时抛出
     */
    public static Long getCurrentPatientId() {
        PatientUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getPatientId();
    }

    /**
     * 获取当前认证用户的用户名
     *
     * @return 用户名
     * @throws BusinessException 未认证时抛出
     */
    public static String getCurrentUsername() {
        PatientUserDetails userDetails = getCurrentUserDetails();
        return userDetails.getUsername();
    }

    /**
     * 获取当前认证的 PatientUserDetails
     *
     * @return PatientUserDetails
     * @throws BusinessException 未认证或 principal 类型不匹配时抛出
     */
    public static PatientUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof PatientUserDetails userDetails)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "认证信息格式异常，请重新登录");
        }

        return userDetails;
    }

    /**
     * 尝试获取当前患者ID，未认证时返回 null（不抛异常）
     *
     * @return 患者ID，或 null
     */
    public static Long tryGetCurrentPatientId() {
        try {
            return getCurrentPatientId();
        } catch (BusinessException e) {
            return null;
        }
    }
}
