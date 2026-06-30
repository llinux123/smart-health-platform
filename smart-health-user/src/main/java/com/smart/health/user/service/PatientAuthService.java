package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.user.config.JwtTokenProvider;
import com.smart.health.user.dto.LoginRequest;
import com.smart.health.user.dto.LoginResponse;
import com.smart.health.user.dto.ProfileResponse;
import com.smart.health.user.dto.RegisterRequest;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 患者认证服务
 */
@Service
@RequiredArgsConstructor
public class PatientAuthService {

    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * 患者注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (patientMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        // 检查身份证号是否已注册
        if (patientMapper.countByIdCard(request.getIdCard()) > 0) {
            throw new BusinessException(ResultCode.ID_CARD_EXISTS);
        }

        // 检查手机号是否已注册
        if (patientMapper.countByPhone(request.getPhone()) > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }

        Patient patient = new Patient();
        patient.setUsername(request.getUsername());
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setRealName(request.getRealName());
        patient.setIdCard(request.getIdCard());
        patient.setPhone(request.getPhone());
        patient.setGender(request.getGender() != null ? request.getGender() : 0);

        patientMapper.insert(patient);
    }

    /**
     * 患者登录
     */
    public LoginResponse login(LoginRequest request) {
        // 通过 AuthenticationManager 验证用户名密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 查询用户信息
        Patient patient = patientMapper.findByUsername(request.getUsername());

        // 生成JWT Token
        String token = jwtTokenProvider.generateToken(patient.getId(), patient.getUsername());

        return LoginResponse.builder()
                .token(token)
                .patientId(patient.getId())
                .username(patient.getUsername())
                .realName(patient.getRealName())
                .build();
    }

    /**
     * 获取当前患者信息
     */
    public ProfileResponse getProfile() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return ProfileResponse.builder()
                .id(patient.getId())
                .username(patient.getUsername())
                .realName(patient.getRealName())
                .idCard(maskIdCard(patient.getIdCard()))
                .phone(maskPhone(patient.getPhone()))
                .gender(patient.getGender())
                .createTime(patient.getCreateTime())
                .build();
    }

    /**
     * 身份证号脱敏：保留前3位和后4位
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 手机号脱敏：保留前3位和后4位
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
