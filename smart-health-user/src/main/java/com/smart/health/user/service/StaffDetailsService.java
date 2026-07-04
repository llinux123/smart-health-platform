package com.smart.health.user.service;

import com.smart.health.common.security.StaffUserDetails;
import com.smart.health.user.entity.Staff;
import com.smart.health.user.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 员工详情服务 - Spring Security UserDetailsService 实现
 */
@Service
@RequiredArgsConstructor
public class StaffDetailsService implements UserDetailsService {

    private final StaffMapper staffMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Staff staff = staffMapper.findByUsername(username);
        if (staff == null) {
            throw new UsernameNotFoundException("员工不存在: " + username);
        }

        String authorityRole = "ROLE_" + staff.getRole();
        return new StaffUserDetails(
                staff.getId(),
                staff.getUsername(),
                staff.getPassword(),
                staff.getRole(),
                staff.getDoctorId(),
                Collections.singletonList(new SimpleGrantedAuthority(authorityRole))
        );
    }
}
