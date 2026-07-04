package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.user.config.JwtTokenProvider;
import com.smart.health.user.dto.LoginResponse;
import com.smart.health.user.dto.StaffRequest;
import com.smart.health.user.dto.StaffVO;
import com.smart.health.user.entity.Staff;
import com.smart.health.user.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 员工认证与管理服务
 */
@Service
@RequiredArgsConstructor
public class StaffAuthService {

    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 员工登录
     */
    public LoginResponse login(String username, String password) {
        Staff staff = staffMapper.findByUsername(username);
        if (staff == null) {
            throw new BusinessException(ResultCode.STAFF_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, staff.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = jwtTokenProvider.generateStaffToken(
                staff.getId(), staff.getUsername(), staff.getRole(), staff.getDoctorId());

        return LoginResponse.builder()
                .token(token)
                .userId(staff.getId())
                .username(staff.getUsername())
                .realName(staff.getRealName())
                .role(staff.getRole())
                .doctorId(staff.getDoctorId())
                .build();
    }

    /**
     * 创建员工
     */
    @Transactional
    public StaffVO createStaff(StaffRequest request) {
        if (staffMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "创建员工时密码为必填");
        }

        Staff staff = new Staff();
        staff.setUsername(request.getUsername());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setRealName(request.getRealName());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole());
        staff.setDoctorId(request.getDoctorId());

        staffMapper.insert(staff);
        return StaffVO.from(staff);
    }

    /**
     * 获取员工列表
     */
    public List<StaffVO> listStaff() {
        return staffMapper.findAll().stream()
                .map(StaffVO::from)
                .toList();
    }

    /**
     * 获取员工详情
     */
    public StaffVO getStaff(Long id) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) {
            throw new BusinessException(ResultCode.STAFF_NOT_FOUND);
        }
        return StaffVO.from(staff);
    }

    /**
     * 更新员工信息（不允许修改角色）
     */
    @Transactional
    public StaffVO updateStaff(Long id, StaffRequest request) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) {
            throw new BusinessException(ResultCode.STAFF_NOT_FOUND);
        }

        // 角色不可修改
        if (request.getRole() != null && !request.getRole().equals(staff.getRole())) {
            throw new BusinessException(ResultCode.ROLE_NOT_MODIFIABLE);
        }

        staff.setRealName(request.getRealName());
        staff.setPhone(request.getPhone());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            staff.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getDoctorId() != null) {
            staff.setDoctorId(request.getDoctorId());
        }

        staffMapper.update(staff);
        return StaffVO.from(staff);
    }

    /**
     * 软删除员工
     */
    @Transactional
    public void deleteStaff(Long id) {
        Staff staff = staffMapper.findById(id);
        if (staff == null) {
            throw new BusinessException(ResultCode.STAFF_NOT_FOUND);
        }
        staffMapper.softDelete(id);
    }
}
