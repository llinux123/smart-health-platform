package com.smart.health.user.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者用户实体
 */
@Data
public class Patient {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String idCard;
    private String phone;
    private Integer gender;
    private String email;
    private String avatar;
    private LocalDate birthday;
    /** 实名认证状态 (0:未认证 1:审核中 2:已认证 3:已拒绝) */
    private Integer idCardStatus;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String faceRecognitionUrl;
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createTime;
}
