package com.smart.health.user.service;

import com.smart.health.common.security.PatientUserDetails;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 患者详情服务 - Spring Security UserDetailsService 实现
 */
@Service
@RequiredArgsConstructor
public class PatientDetailsService implements UserDetailsService {

    private final PatientMapper patientMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Patient patient = patientMapper.findByUsername(username);
        if (patient == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return new PatientUserDetails(
                patient.getId(),
                patient.getUsername(),
                patient.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
    }
}
