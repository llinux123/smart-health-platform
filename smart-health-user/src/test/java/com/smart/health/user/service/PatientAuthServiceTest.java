package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.common.security.PatientUserDetails;
import com.smart.health.user.config.JwtTokenProvider;
import com.smart.health.user.dto.BindIdentityRequest;
import com.smart.health.user.dto.LoginResponse;
import com.smart.health.user.dto.ProfileResponse;
import com.smart.health.user.dto.RegisterRequest;
import com.smart.health.user.dto.ResetPasswordRequest;
import com.smart.health.user.dto.SmsLoginRequest;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.mapper.PatientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientAuthService 单元测试")
class PatientAuthServiceTest {

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SmsService smsService;

    private PatientAuthService patientAuthService;

    @BeforeEach
    void setUp() {
        patientAuthService = new PatientAuthService(
                patientMapper, passwordEncoder, jwtTokenProvider, smsService
        );
        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("注册成功 - 正常流程")
    void register_success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setPhone("13800138000");
        request.setGender(1);

        when(patientMapper.countByUsername("testuser")).thenReturn(0);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(0);
        when(patientMapper.countByPhone("13800138000")).thenReturn(0);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(patientMapper.insert(any())).thenReturn(1);

        // When
        patientAuthService.register(request);

        // Then
        verify(patientMapper).insert(any());
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void register_usernameExists_throwsException() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setPhone("13800138000");

        when(patientMapper.countByUsername("existinguser")).thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.USER_EXISTS.getCode());

        verify(patientMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册失败 - 身份证号已注册 (ID_CARD_EXISTS)")
    void register_idCardExists_throwsException() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setPhone("13800138000");

        when(patientMapper.countByUsername("testuser")).thenReturn(0);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.ID_CARD_EXISTS.getCode());

        verify(patientMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册失败 - 手机号已注册 (PHONE_EXISTS)")
    void register_phoneExists_throwsException() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setPhone("13800138000");

        when(patientMapper.countByUsername("testuser")).thenReturn(0);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(0);
        when(patientMapper.countByPhone("13800138000")).thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.PHONE_EXISTS.getCode());

        verify(patientMapper, never()).insert(any());
    }

    @Test
    @DisplayName("获取患者信息成功")
    void getProfile_success() {
        // Given
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("testuser");
        patient.setRealName("张三");
        patient.setIdCard("110101199001011234");
        patient.setPhone("13800138000");
        patient.setGender(1);
        patient.setCreateTime(LocalDateTime.now());

        PatientUserDetails userDetails = new PatientUserDetails(1L, "testuser", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(patientMapper.findById(1L)).thenReturn(patient);

        // When
        ProfileResponse response = patientAuthService.getProfile();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getRealName()).isEqualTo("张三");
        assertThat(response.getAvatar()).isNull();
        assertThat(response.getIdCardStatus()).isNull();
        // 验证脱敏
        assertThat(response.getIdCard()).isNotEqualTo("110101199001011234");
        assertThat(response.getIdCard()).startsWith("110");
        assertThat(response.getPhone()).startsWith("138");
        assertThat(response.getPhone()).isNotEqualTo("13800138000");
    }

    @Test
    @DisplayName("获取患者信息失败 - 用户不存在")
    void getProfile_userNotFound_throwsException() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(99L, "nonexistent", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(patientMapper.findById(99L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.getProfile())
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("短信验证码登录 - 已注册患者直接登录")
    void smsLogin_existingUser_success() {
        // Given
        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("u_13800138000");
        patient.setPhone("13800138000");
        patient.setRealName("张三");

        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(patientMapper.findByPhone("13800138000")).thenReturn(patient);
        when(jwtTokenProvider.generatePatientToken(1L, "u_13800138000")).thenReturn("token-123");

        // When
        LoginResponse response = patientAuthService.smsLogin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token-123");
        assertThat(response.getIsNewUser()).isFalse();
        assertThat(response.getRandomPassword()).isNull();
    }

    @Test
    @DisplayName("短信验证码登录 - 未注册患者自动创建账号并返回初始密码")
    void smsLogin_newUser_autoRegisterAndReturnRandomPassword() {
        // Given
        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13900139000");
        request.setCode("654321");

        when(smsService.verifyCode("13900139000", "654321")).thenReturn(true);
        when(patientMapper.findByPhone("13900139000")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(patientMapper.insert(any())).thenReturn(1);
        when(jwtTokenProvider.generatePatientToken(any(), any())).thenReturn("token-new");

        // When
        LoginResponse response = patientAuthService.smsLogin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIsNewUser()).isTrue();
        assertThat(response.getRandomPassword()).isNotNull().hasSize(12);
        verify(patientMapper).insert(any());
    }

    @Test
    @DisplayName("短信验证码登录 - 验证码错误时拒绝登录")
    void smsLogin_wrongCode_throwsException() {
        // Given
        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13800138000");
        request.setCode("000000");

        when(smsService.verifyCode("13800138000", "000000")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.smsLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SMS_CODE_ERROR.getCode());
    }

    @Test
    @DisplayName("身份绑定 - 更新实名信息成功")
    void bindIdentity_success() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(1L, "u_13800138000", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("u_13800138000");
        patient.setRealName("138****8000");
        patient.setPhone("13800138000");

        BindIdentityRequest request = new BindIdentityRequest();
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setGender(1);
        request.setEmail("zhangsan@example.com");

        when(patientMapper.findById(1L)).thenReturn(patient);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(0);
        when(patientMapper.update(any())).thenReturn(1);

        // When
        ProfileResponse response = patientAuthService.bindIdentity(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRealName()).isEqualTo("张三");
        assertThat(response.getEmail()).isEqualTo("zhangsan@example.com");
        assertThat(response.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.getIdCardStatus()).isEqualTo(1); // 审核中
    }

    @Test
    @DisplayName("身份绑定 - 测试环境跳过验证直接通过")
    void bindIdentity_skipVerification_directVerified() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(1L, "u_13800138000", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("u_13800138000");
        patient.setRealName("138****8000");
        patient.setPhone("13800138000");

        BindIdentityRequest request = new BindIdentityRequest();
        request.setRealName("张三");
        request.setIdCard("110101199001011234");
        request.setGender(1);
        request.setSkipVerification(true);

        when(patientMapper.findById(1L)).thenReturn(patient);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(0);
        when(patientMapper.update(any())).thenReturn(1);

        // When
        ProfileResponse response = patientAuthService.bindIdentity(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getIdCardStatus()).isEqualTo(2); // 已认证
        assertThat(response.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("更新用户名 - 成功")
    void updateUsername_success() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(1L, "olduser", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("olduser");
        patient.setRealName("张三");
        patient.setPhone("13800138000");

        when(patientMapper.findById(1L)).thenReturn(patient);
        when(patientMapper.countByUsername("newuser")).thenReturn(0);
        when(patientMapper.update(any())).thenReturn(1);

        // When
        ProfileResponse response = patientAuthService.updateUsername("newuser");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
    }

    @Test
    @DisplayName("更新头像 - 成功")
    void updateAvatar_success() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(1L, "testuser", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("testuser");
        patient.setRealName("张三");
        patient.setPhone("13800138000");

        when(patientMapper.findById(1L)).thenReturn(patient);
        when(patientMapper.update(any())).thenReturn(1);

        // When
        ProfileResponse response = patientAuthService.updateAvatar("https://example.com/avatar.png");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAvatar()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    @DisplayName("重置密码 - 验证码通过后更新密码")
    void resetPassword_success() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone("13800138000");
        request.setVerifyCode("123456");
        request.setNewPassword("newPass123");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPhone("13800138000");

        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(patientMapper.findByPhone("13800138000")).thenReturn(patient);
        when(passwordEncoder.encode("newPass123")).thenReturn("$2a$10$encoded");
        when(patientMapper.updatePasswordByPhone("13800138000", "$2a$10$encoded")).thenReturn(1);

        // When
        patientAuthService.resetPassword(request);

        // Then
        verify(patientMapper).updatePasswordByPhone("13800138000", "$2a$10$encoded");
    }

    @Test
    @DisplayName("短信验证码登录 - 账号已禁用")
    void smsLogin_disabledUser_throwsException() {
        // Given
        SmsLoginRequest request = new SmsLoginRequest();
        request.setPhone("13800138000");
        request.setCode("123456");

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setPhone("13800138000");
        patient.setIsDeleted(1);

        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(patientMapper.findByPhone("13800138000")).thenReturn(patient);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.smsLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.ACCOUNT_DISABLED.getCode());
    }

    @Test
    @DisplayName("身份绑定 - 身份证已被其他用户绑定")
    void bindIdentity_idCardExists_throwsException() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(1L, "u_13800138000", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Patient patient = new Patient();
        patient.setId(1L);
        patient.setUsername("u_13800138000");

        BindIdentityRequest request = new BindIdentityRequest();
        request.setRealName("张三");
        request.setIdCard("110101199001011234");

        when(patientMapper.findById(1L)).thenReturn(patient);
        when(patientMapper.countByIdCard("110101199001011234")).thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.bindIdentity(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.ID_CARD_EXISTS.getCode());
    }

    @Test
    @DisplayName("身份绑定 - 用户不存在")
    void bindIdentity_userNotFound_throwsException() {
        // Given
        PatientUserDetails userDetails = new PatientUserDetails(99L, "nonexistent", "password", Collections.emptyList());
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        BindIdentityRequest request = new BindIdentityRequest();
        request.setRealName("张三");
        request.setIdCard("110101199001011234");

        when(patientMapper.findById(99L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.bindIdentity(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("重置密码 - 验证码错误时拒绝重置")
    void resetPassword_wrongCode_throwsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone("13800138000");
        request.setVerifyCode("000000");
        request.setNewPassword("newPass123");

        when(smsService.verifyCode("13800138000", "000000")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SMS_CODE_ERROR.getCode());
    }

    @Test
    @DisplayName("重置密码 - 用户不存在")
    void resetPassword_userNotFound_throwsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone("13800138000");
        request.setVerifyCode("123456");
        request.setNewPassword("newPass123");

        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(patientMapper.findByPhone("13800138000")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> patientAuthService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.USER_NOT_FOUND.getCode());
    }
}
