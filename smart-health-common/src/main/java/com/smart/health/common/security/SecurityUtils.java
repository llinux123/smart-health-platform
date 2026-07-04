package com.smart.health.common.security;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具类
 * 从 SecurityContextHolder 中提取当前认证用户的信息（患者或员工）
 * <p>
 * 同时支持两种认证主体：
 * <ul>
 *   <li>{@link PatientUserDetails} / {@link StaffUserDetails} — user-service 基于 DB 的完整认证</li>
 *   <li>{@link JwtPrincipal} — 其他微服务仅基于 JWT claims 的轻量认证</li>
 * </ul>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    // ==================== 通用方法 ====================

    /**
     * 获取当前认证用户的用户名
     */
    public static String getCurrentUsername() {
        Object principal = getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof JwtPrincipal jwt) {
            return jwt.getUsername();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取用户名");
    }

    /**
     * 获取当前认证用户的角色（PATIENT / DOCTOR / PHARMACIST / ADMIN）
     */
    public static String getCurrentRole() {
        Object principal = getPrincipal();
        if (principal instanceof PatientUserDetails) {
            return "PATIENT";
        }
        if (principal instanceof StaffUserDetails staffDetails) {
            return staffDetails.getRole();
        }
        if (principal instanceof JwtPrincipal jwt) {
            return jwt.getRole();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "无法识别的用户类型");
    }

    /**
     * 判断当前用户是否为指定角色
     */
    public static boolean hasRole(String role) {
        try {
            return getCurrentRole().equals(role);
        } catch (BusinessException e) {
            return false;
        }
    }

    // ==================== 患者专用方法 ====================

    /**
     * 获取当前认证患者的ID
     *
     * @return 患者ID
     * @throws BusinessException 未认证或非患者时抛出
     */
    public static Long getCurrentPatientId() {
        Object principal = getPrincipal();
        if (principal instanceof PatientUserDetails patientDetails) {
            return patientDetails.getPatientId();
        }
        if (principal instanceof JwtPrincipal jwt && "PATIENT".equals(jwt.getRole())) {
            return jwt.getUserId();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "当前非患者身份");
    }

    /**
     * 尝试获取当前患者ID，未认证或非患者时返回 null（不抛异常）
     */
    public static Long tryGetCurrentPatientId() {
        try {
            return getCurrentPatientId();
        } catch (BusinessException e) {
            return null;
        }
    }

    // ==================== 员工专用方法 ====================

    /**
     * 获取当前认证员工的ID
     */
    public static Long getCurrentStaffId() {
        Object principal = getPrincipal();
        if (principal instanceof StaffUserDetails staffDetails) {
            return staffDetails.getStaffId();
        }
        if (principal instanceof JwtPrincipal jwt) {
            return jwt.getUserId();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "当前非员工身份");
    }

    /**
     * 获取当前认证员工的关联医生ID（仅 DOCTOR 角色）
     */
    public static Long getCurrentDoctorId() {
        Object principal = getPrincipal();
        if (principal instanceof StaffUserDetails staffDetails) {
            return staffDetails.getDoctorId();
        }
        if (principal instanceof JwtPrincipal jwt) {
            return jwt.getDoctorId();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "当前非员工身份");
    }

    /**
     * 尝试获取当前员工ID，未认证或非员工时返回 null（不抛异常）
     */
    public static Long tryGetCurrentStaffId() {
        try {
            return getCurrentStaffId();
        } catch (BusinessException e) {
            return null;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 获取当前认证的 principal 对象（通用）
     */
    private static Object getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return authentication.getPrincipal();
    }

    /**
     * 获取当前认证的 UserDetails（兼容旧接口）
     */
    public static UserDetails getCurrentUserDetails() {
        Object principal = getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "认证信息格式异常，请重新登录");
    }

    /**
     * 获取患者 UserDetails
     */
    private static PatientUserDetails getPatientUserDetails() {
        UserDetails userDetails = getCurrentUserDetails();
        if (!(userDetails instanceof PatientUserDetails patientDetails)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前非患者身份");
        }
        return patientDetails;
    }

    /**
     * 获取员工 UserDetails
     */
    private static StaffUserDetails getStaffUserDetails() {
        UserDetails userDetails = getCurrentUserDetails();
        if (!(userDetails instanceof StaffUserDetails staffDetails)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前非员工身份");
        }
        return staffDetails;
    }
}
