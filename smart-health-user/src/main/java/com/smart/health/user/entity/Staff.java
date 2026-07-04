package com.smart.health.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工用户实体（医生/药师/运维）
 */
@Data
public class Staff {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String role;
    private Long doctorId;
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
