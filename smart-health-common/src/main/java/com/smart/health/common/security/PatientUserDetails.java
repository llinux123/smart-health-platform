package com.smart.health.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 携带患者ID的 UserDetails 实现
 * 用于在 SecurityContext 中传递患者ID到各业务模块
 */
@Getter
public class PatientUserDetails extends User {

    private final Long patientId;

    public PatientUserDetails(Long patientId, String username, String password,
                              Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.patientId = patientId;
    }
}
