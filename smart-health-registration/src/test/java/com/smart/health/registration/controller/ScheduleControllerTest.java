package com.smart.health.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.security.PatientUserDetails;
import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.mapper.DoctorMapper;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.mapper.RegistrationOrderMapper;
import com.smart.health.registration.service.RegistrationOrderService;
import com.smart.health.registration.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScheduleController.class)
@DisplayName("ScheduleController 单元测试")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private RegistrationOrderService registrationOrderService;

    @MockBean
    private DoctorMapper doctorMapper;

    @MockBean
    private DoctorScheduleMapper doctorScheduleMapper;

    @MockBean
    private RegistrationOrderMapper registrationOrderMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUpSecurityContext() {
        PatientUserDetails userDetails = new PatientUserDetails(
                100L, "testpatient", "password",
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
        auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("秒杀抢号 - 成功返回订单号")
    void seckill_success() throws Exception {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);

        SeckillResponse response = new SeckillResponse("REG_20260629_000001", "QUEUING");
        when(scheduleService.seckill(any(), eq(100L))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/registration/seckill")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("REG_20260629_000001"))
                .andExpect(jsonPath("$.data.status").value("QUEUING"));
    }

    @Test
    @DisplayName("秒杀抢号 - 参数校验失败")
    void seckill_validationError() throws Exception {
        // Given
        SeckillRequest request = new SeckillRequest();
        // 缺少必填字段

        // When & Then
        mockMvc.perform(post("/api/v1/registration/seckill")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("查询订单详情 - 成功")
    void getOrderDetail_success() throws Exception {
        // Given
        OrderVO order = new OrderVO();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setStatus(0);

        when(registrationOrderService.getOrderVOByOrderSn(
                eq("REG_20260629_000001"), eq(100L), eq("PATIENT"))).thenReturn(order);

        // When & Then
        mockMvc.perform(get("/api/v1/registration/order/detail")
                        .with(authentication(auth))
                        .param("orderSn", "REG_20260629_000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("REG_20260629_000001"))
                .andExpect(jsonPath("$.data.patientId").value(100));
    }

    @Test
    @DisplayName("查询当前患者订单列表 - 成功")
    void listOrders_success() throws Exception {
        // Given
        OrderVO order1 = new OrderVO();
        order1.setOrderSn("REG_20260629_000001");
        order1.setPatientId(100L);

        OrderVO order2 = new OrderVO();
        order2.setOrderSn("REG_20260629_000002");
        order2.setPatientId(100L);

        List<OrderVO> orders = Arrays.asList(order1, order2);
        when(registrationOrderService.listOrderVOByPatientId(100L)).thenReturn(orders);

        // When & Then — patientId 从 SecurityContext 获取，不再作为请求参数传入
        mockMvc.perform(get("/api/v1/registration/order/list")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
