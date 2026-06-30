package com.smart.health.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.service.RegistrationOrderService;
import com.smart.health.registration.service.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("秒杀抢号 - 成功返回订单号")
    void seckill_success() throws Exception {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        request.setPatientId(100L);

        SeckillResponse response = new SeckillResponse("REG_20260629_000001", "QUEUING");
        when(scheduleService.seckill(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/registration/seckill")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("查询订单详情 - 成功")
    void getOrderDetail_success() throws Exception {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setAmount(new BigDecimal("50.00"));
        order.setStatus(0);

        when(registrationOrderService.getByOrderSn("REG_20260629_000001")).thenReturn(order);

        // When & Then
        mockMvc.perform(get("/api/v1/registration/order/detail")
                        .param("orderSn", "REG_20260629_000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("REG_20260629_000001"))
                .andExpect(jsonPath("$.data.patientId").value(100));
    }

    @Test
    @DisplayName("查询患者订单列表 - 成功")
    void listOrders_success() throws Exception {
        // Given
        RegistrationOrder order1 = new RegistrationOrder();
        order1.setOrderSn("REG_20260629_000001");
        order1.setPatientId(100L);

        RegistrationOrder order2 = new RegistrationOrder();
        order2.setOrderSn("REG_20260629_000002");
        order2.setPatientId(100L);

        List<RegistrationOrder> orders = Arrays.asList(order1, order2);
        when(registrationOrderService.listByPatientId(100L)).thenReturn(orders);

        // When & Then
        mockMvc.perform(get("/api/v1/registration/order/list")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
