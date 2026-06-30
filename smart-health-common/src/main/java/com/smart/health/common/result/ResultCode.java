package com.smart.health.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 认证相关 401-403
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有操作权限"),
    TOKEN_INVALID(401, "无效的Token"),

    // 参数校验 400
    PARAM_ERROR(400, "参数错误"),

    // 业务相关 1001-1999
    USER_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PHONE_EXISTS(1004, "手机号已注册"),
    ID_CARD_EXISTS(1005, "身份证号已注册"),

    // 挂号相关 2001-2999
    SCHEDULE_NOT_FOUND(2001, "排班信息不存在"),
    STOCK_EMPTY(2002, "号源已抢完"),
    REPEAT_SECKILL(2003, "请勿重复抢号"),
    SECKILL_FAIL(2004, "抢号失败，请重试"),

    // 处方相关 3001-3999
    PRESCRIPTION_NOT_FOUND(3001, "处方不存在"),
    STOCK_NOT_ENOUGH(3002, "药品库存不足"),
    AUDIT_STATUS_ERROR(3003, "处方审核状态异常"),
    PRESCRIPTION_ALREADY_AUDITED(3004, "处方已审核，不可重复操作"),

    // 问诊相关 4001-4999
    SESSION_NOT_FOUND(4001, "问诊会话不存在"),
    AI_SERVICE_UNAVAILABLE(4002, "AI服务暂不可用"),
    RAG_SEARCH_FAIL(4003, "知识库检索失败");

    private final Integer code;
    private final String message;
}
