package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.user.config.JwtTokenProvider;
import com.smart.health.user.dto.*;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 患者认证服务
 */
@Service
@RequiredArgsConstructor
public class PatientAuthService {

    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SmsService smsService;

    private static final String RANDOM_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_PASSWORD_LENGTH = 12;

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
        Patient patient = patientMapper.findByUsername(request.getUsername());
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        if (patient.getIsDeleted() != null && patient.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 生成JWT Token
        String token = jwtTokenProvider.generatePatientToken(patient.getId(), patient.getUsername());

        return LoginResponse.builder()
                .token(token)
                .userId(patient.getId())
                .patientId(patient.getId())
                .username(patient.getUsername())
                .realName(patient.getRealName())
                .role("PATIENT")
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
                .email(patient.getEmail())
                .avatar(patient.getAvatar())
                .birthday(patient.getBirthday())
                .idCardStatus(patient.getIdCardStatus())
                .idCardFrontUrl(patient.getIdCardFrontUrl())
                .idCardBackUrl(patient.getIdCardBackUrl())
                .faceRecognitionUrl(patient.getFaceRecognitionUrl())
                .createTime(patient.getCreateTime())
                .build();
    }

    /**
     * 发送短信验证码
     */
    public void sendSmsCode(String phone) {
        smsService.sendCode(phone);
    }

    /**
     * 短信验证码登录（登录注册合一）
     * 手机号不存在时自动创建账号，返回随机密码
     */
    @Transactional
    public LoginResponse smsLogin(SmsLoginRequest request) {
        // 1. 验证短信验证码
        if (!smsService.verifyCode(request.getPhone(), request.getCode())) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }

        // 2. 查询用户是否已存在
        Patient patient = patientMapper.findByPhone(request.getPhone());
        boolean isNewUser = (patient == null);

        // 3. 新用户自动注册
        if (isNewUser) {
            patient = new Patient();
            String phone = request.getPhone();
            patient.setUsername("u_" + phone);
            String randomPassword = generateRandomPassword();
            patient.setPassword(passwordEncoder.encode(randomPassword));
            patient.setRealName(phone.substring(0, 3) + "****" + phone.substring(7));
            patient.setPhone(phone);
            patient.setGender(0);
            patientMapper.insert(patient);

            // 生成Token
            String token = jwtTokenProvider.generatePatientToken(patient.getId(), patient.getUsername());

            return LoginResponse.builder()
                    .token(token)
                    .userId(patient.getId())
                    .patientId(patient.getId())
                    .username(patient.getUsername())
                    .realName(patient.getRealName())
                    .role("PATIENT")
                    .isNewUser(true)
                    .randomPassword(randomPassword)
                    .build();
        }

        // 4. 已有用户直接登录
        if (patient.getIsDeleted() != null && patient.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtTokenProvider.generatePatientToken(patient.getId(), patient.getUsername());

        return LoginResponse.builder()
                .token(token)
                .userId(patient.getId())
                .patientId(patient.getId())
                .username(patient.getUsername())
                .realName(patient.getRealName())
                .role("PATIENT")
                .isNewUser(false)
                .build();
    }

    /**
     * 身份绑定（更新患者实名信息 + 邮箱）
     */
    @Transactional
    public ProfileResponse bindIdentity(BindIdentityRequest request) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查身份证号是否被其他用户绑定
        if (request.getIdCard() != null
                && !request.getIdCard().equals(patient.getIdCard())
                && patientMapper.countByIdCard(request.getIdCard()) > 0) {
            throw new BusinessException(ResultCode.ID_CARD_EXISTS);
        }

        patient.setRealName(request.getRealName());
        patient.setIdCard(request.getIdCard());
        patient.setGender(request.getGender() != null ? request.getGender() : patient.getGender());
        patient.setEmail(request.getEmail());
        patient.setBirthday(parseBirthdayFromIdCard(request.getIdCard()));
        patient.setIdCardFrontUrl(request.getIdCardFrontUrl());
        patient.setIdCardBackUrl(request.getIdCardBackUrl());
        patient.setFaceRecognitionUrl(request.getFaceRecognitionUrl());
        // 测试环境跳过审核时直接设为已认证，否则进入审核中
        patient.setIdCardStatus(Boolean.TRUE.equals(request.getSkipVerification()) ? 2 : 1);
        patientMapper.update(patient);

        return ProfileResponse.builder()
                .id(patient.getId())
                .username(patient.getUsername())
                .realName(patient.getRealName())
                .idCard(maskIdCard(patient.getIdCard()))
                .phone(maskPhone(patient.getPhone()))
                .gender(patient.getGender())
                .email(patient.getEmail())
                .avatar(patient.getAvatar())
                .birthday(patient.getBirthday())
                .idCardStatus(patient.getIdCardStatus())
                .idCardFrontUrl(patient.getIdCardFrontUrl())
                .idCardBackUrl(patient.getIdCardBackUrl())
                .faceRecognitionUrl(patient.getFaceRecognitionUrl())
                .createTime(patient.getCreateTime())
                .build();
    }

    /**
     * 更新用户名
     */
    @Transactional
    public ProfileResponse updateUsername(String username) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!patient.getUsername().equals(username) && patientMapper.countByUsername(username) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        patient.setUsername(username);
        patientMapper.update(patient);
        return getProfile();
    }

    /**
     * 更新头像
     */
    @Transactional
    public ProfileResponse updateAvatar(String avatarUrl) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        patient.setAvatar(avatarUrl);
        patientMapper.update(patient);
        return getProfile();
    }

    /**
     * 从身份证号解析生日
     */
    private LocalDate parseBirthdayFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) {
            return null;
        }
        try {
            String birthdayStr = idCard.substring(6, 14);
            return LocalDate.parse(birthdayStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * 重置密码（短信验证码验证后）
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 验证短信验证码
        if (!smsService.verifyCode(request.getPhone(), request.getVerifyCode())) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }

        // 2. 查询用户
        Patient patient = patientMapper.findByPhone(request.getPhone());
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 3. 更新密码
        patientMapper.updatePasswordByPhone(request.getPhone(),
                passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 生成随机密码（含大小写字母+数字，12位）
     */
    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(RANDOM_PASSWORD_LENGTH);
        for (int i = 0; i < RANDOM_PASSWORD_LENGTH; i++) {
            sb.append(RANDOM_PASSWORD_CHARS.charAt(random.nextInt(RANDOM_PASSWORD_CHARS.length())));
        }
        return sb.toString();
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
