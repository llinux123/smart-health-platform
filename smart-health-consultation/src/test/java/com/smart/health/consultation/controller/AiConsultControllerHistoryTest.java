package com.smart.health.consultation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.dto.ConsultStreamResponse;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.service.ConsultationService;
import com.smart.health.consultation.service.MultimodalService;
import com.smart.health.common.security.PatientUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiConsultControllerHistoryTest {

    @Mock
    private MultimodalService multimodalService;
    @Mock
    private ConsultationService consultationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AiConsultController controller = new AiConsultController(multimodalService, consultationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new PatientUserDetails(0L, "test", "pwd", Collections.emptyList());
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/ai/sessions/{sessionSn}/history matches issue #21 response shape")
    void getSessionHistory_returnsIssue21Shape() throws Exception {
        List<ConsultStreamResponse.Citation> citations = List.of(
                ConsultStreamResponse.Citation.builder()
                        .title("Guide A")
                        .category("Clinical Guideline")
                        .snippet("Sample snippet")
                        .build()
        );
        when(consultationService.getSessionHistory(eq("session_abc"), eq(0L)))
                .thenReturn(List.of(
                        SessionHistoryVO.builder()
                                .role("user")
                                .content("question")
                                .timestamp("2026-06-29T10:00:00")
                                .build(),
                        SessionHistoryVO.builder()
                                .role("assistant")
                                .content("answer")
                                .timestamp("2026-06-29T10:00:01")
                                .citations(citations)
                                .build()
                ));

        String body = mockMvc.perform(get("/api/v1/ai/sessions/session_abc/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        JsonNode messages = root.path("data");
        assertThat(messages.isArray()).isTrue();
        assertThat(messages).hasSize(2);

        JsonNode user = messages.get(0);
        assertThat(user.path("role").asText()).isEqualTo("user");
        assertThat(user.path("timestamp").asText()).isEqualTo("2026-06-29T10:00:00");
        assertThat(user.has("createTime")).isFalse();

        JsonNode assistant = messages.get(1);
        assertThat(assistant.path("role").asText()).isEqualTo("assistant");
        assertThat(assistant.path("timestamp").asText()).isEqualTo("2026-06-29T10:00:01");
        assertThat(assistant.has("createTime")).isFalse();
        assertThat(assistant.path("citations").isArray()).isTrue();
        assertThat(assistant.path("citations").get(0).path("title").asText()).isEqualTo("Guide A");
        assertThat(assistant.path("citations").get(0).path("category").asText()).isEqualTo("Clinical Guideline");
        assertThat(assistant.path("citations").get(0).path("snippet").asText()).isEqualTo("Sample snippet");
    }
}
