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
    ACCOUNT_DISABLED(1006, "账号已被禁用"),
    INVALID_LOGIN_TYPE(1007, "无效的登录类型，仅支持 PATIENT / STAFF"),
    STAFF_NOT_FOUND(1008, "员工不存在"),
    ROLE_NOT_MODIFIABLE(1009, "角色创建后不可修改"),

    // 挂号相关 2001-2999
    SCHEDULE_NOT_FOUND(2001, "排班信息不存在"),
    DOCTOR_NOT_FOUND(2005, "医生不存在"),
    STOCK_EMPTY(2002, "号源已抢完"),
    REPEAT_SECKILL(2003, "请勿重复抢号"),
    SECKILL_FAIL(2004, "抢号失败，请重试"),
    ORDER_NOT_FOUND(2006, "订单不存在"),
    ORDER_STATUS_ERROR(2007, "订单状态异常，无法操作"),

    // 处方相关 3001-3999
    PRESCRIPTION_NOT_FOUND(3001, "处方不存在"),
    STOCK_NOT_ENOUGH(3002, "药品库存不足"),
    AUDIT_STATUS_ERROR(3003, "处方审核状态异常"),
    PRESCRIPTION_ALREADY_AUDITED(3004, "处方已审核，不可重复操作"),

    // 问诊相关 4001-4999
    SESSION_NOT_FOUND(4001, "问诊会话不存在"),
    AI_SERVICE_UNAVAILABLE(4002, "AI服务暂不可用"),
    RAG_SEARCH_FAIL(4003, "知识库检索失败"),
    SESSION_COMPLETED(4004, "问诊已结束，无法继续"),
    SESSION_DELETED(4005, "会话已删除"),
    RATING_EXISTS(4006, "该会话已评分，不可重复评分"),
    RATING_INVALID(4007, "评分必须在1-5之间"),
    TURN_NOT_FOUND(4008, "对话轮次不存在"),
    REGENERATE_NOT_ALLOWED(4009, "仅可重新生成最后一轮对话"),
    SESSION_NOT_DELETED(4010, "会话未在回收站中");

    private final Integer code;
    private final String message;
}
