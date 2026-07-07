package com.smart.health.consultation.constant;

/**
 * 会话状态常量
 *
 * <p>DB 存储为 VARCHAR ("IN_PROGRESS" / "COMPLETED")，
 * 此处提供类型安全常量以避免魔法字符串散布在业务代码中。</p>
 */
public final class SessionStatus {

    private SessionStatus() {}

    /** 进行中 */
    public static final String IN_PROGRESS = "IN_PROGRESS";

    /** 已完成 */
    public static final String COMPLETED = "COMPLETED";

    /** 等待医生接诊 */
    public static final String PENDING_DOCTOR = "PENDING_DOCTOR";

    /** 医生已接诊，沟通中 */
    public static final String DOCTOR_ACTIVE = "DOCTOR_ACTIVE";

    /**
     * 判断会话是否仍在进行中（AI对话阶段）
     */
    public static boolean isInProgress(String status) {
        return IN_PROGRESS.equals(status);
    }

    /**
     * 判断会话是否已结束
     */
    public static boolean isCompleted(String status) {
        return COMPLETED.equals(status);
    }

    /**
     * 判断会话是否处于转诊相关状态（等待医生或医生沟通中）
     */
    public static boolean isHandoffRelated(String status) {
        return PENDING_DOCTOR.equals(status) || DOCTOR_ACTIVE.equals(status);
    }
}
