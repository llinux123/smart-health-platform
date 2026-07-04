package com.smart.health.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 携带员工信息的 UserDetails 实现
 * 用于在 SecurityContext 中传递员工ID、角色到各业务模块
 */
@Getter
public class StaffUserDetails extends User {

    private final Long staffId;
    private final String role;

    /** 仅医生角色有值 */
    private final Long doctorId;

    public StaffUserDetails(Long staffId, String username, String password, String role,
                            Long doctorId, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.staffId = staffId;
        this.role = role;
        this.doctorId = doctorId;
    }
}
