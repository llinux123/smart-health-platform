package com.smart.health.user.entity;

import lombok.Data;
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
    private Integer isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createTime;
}
