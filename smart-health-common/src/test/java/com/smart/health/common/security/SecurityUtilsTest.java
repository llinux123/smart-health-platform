package com.smart.health.common.security;

import com.smart.health.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityUtils 安全上下文工具测试")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentPatientId")
    class GetPatientId {

        @Test
        @DisplayName("SecurityContext 已认证时返回正确的 patientId")
        void authenticated_returnsPatientId() {
            PatientUserDetails userDetails = new PatientUserDetails(
                    42L, "testuser", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
            );
            setAuthentication(userDetails);

            Long patientId = SecurityUtils.getCurrentPatientId();

            assertThat(patientId).isEqualTo(42L);
        }

        @Test
        @DisplayName("SecurityContext 为空时抛出 BusinessException")
        void unauthenticated_throwsBusinessException() {
            assertThatThrownBy(() -> SecurityUtils.getCurrentPatientId())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未登录");
        }
    }

    @Nested
    @DisplayName("getCurrentUsername")
    class GetUsername {

        @Test
        @DisplayName("SecurityContext 已认证时返回正确的 username")
        void authenticated_returnsUsername() {
            PatientUserDetails userDetails = new PatientUserDetails(
                    42L, "zhangsan", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
            );
            setAuthentication(userDetails);

            String username = SecurityUtils.getCurrentUsername();

            assertThat(username).isEqualTo("zhangsan");
        }
    }

    @Nested
    @DisplayName("tryGetCurrentPatientId")
    class TryGetPatientId {

        @Test
        @DisplayName("未认证时返回 null 而非抛异常")
        void unauthenticated_returnsNull() {
            Long patientId = SecurityUtils.tryGetCurrentPatientId();

            assertThat(patientId).isNull();
        }

        @Test
        @DisplayName("已认证时返回 patientId")
        void authenticated_returnsPatientId() {
            PatientUserDetails userDetails = new PatientUserDetails(
                    99L, "testuser", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
            );
            setAuthentication(userDetails);

            Long patientId = SecurityUtils.tryGetCurrentPatientId();

            assertThat(patientId).isEqualTo(99L);
        }
    }

    private void setAuthentication(PatientUserDetails userDetails) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
