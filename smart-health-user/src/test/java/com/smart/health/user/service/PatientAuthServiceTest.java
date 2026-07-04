package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.common.security.PatientUserDetails;
import com.smart.health.user.config.JwtTokenProvider;
import com.smart.health.user.dto.ProfileResponse;
import com.smart.health.user.dto.RegisterRequest;
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

    private PatientAuthService patientAuthService;

    @BeforeEach
    void setUp() {
        patientAuthService = new PatientAuthService(
                patientMapper, passwordEncoder, jwtTokenProvider
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
}
