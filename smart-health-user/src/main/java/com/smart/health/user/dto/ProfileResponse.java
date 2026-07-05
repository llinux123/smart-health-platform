package com.smart.health.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者个人信息响应 DTO
 * 不包含密码字段，敏感信息脱敏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    /** 患者ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 身份证号（脱敏） */
    private String idCard;

    /** 手机号（脱敏） */
    private String phone;

    /** 性别 */
    private Integer gender;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 生日 */
    private LocalDate birthday;

    /** 实名认证状态 (0:未认证 1:审核中 2:已认证 3:已拒绝) */
    private Integer idCardStatus;

    /** 身份证正面URL */
    private String idCardFrontUrl;

    /** 身份证反面URL */
    private String idCardBackUrl;

    /** 人脸识别结果URL */
    private String faceRecognitionUrl;

    /** 注册时间 */
    private LocalDateTime createTime;
}
