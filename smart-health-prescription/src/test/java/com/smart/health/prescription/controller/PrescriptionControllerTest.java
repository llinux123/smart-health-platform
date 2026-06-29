package com.smart.health.prescription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.prescription.dto.InventoryVO;
import com.smart.health.prescription.dto.PrescriptionAuditRequest;
import com.smart.health.prescription.dto.PrescriptionIssueRequest;
import com.smart.health.prescription.dto.PrescriptionItemVO;
import com.smart.health.prescription.dto.PrescriptionVO;
import com.smart.health.prescription.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionController 单元测试")
class PrescriptionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PrescriptionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.smart.health.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/prescription/issue - 成功开具处方")
    void issuePrescription_success() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .patientId(1L)
                .doctorId(99L)
                .diagnosis("感冒")
                .auditStatus(0)
                .status(0)
                .createTime(LocalDateTime.now())
                .items(List.of(PrescriptionItemVO.builder()
                        .medicineName("阿莫西林")
                        .quantity(2)
                        .unit("盒")
                        .build()))
                .build();

        when(prescriptionService.issuePrescription(any(PrescriptionIssueRequest.class), anyLong()))
                .thenReturn(vo);

        PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                .patientId(1L)
                .diagnosis("感冒")
                .medicines(List.of(PrescriptionIssueRequest.MedicineItem.builder()
                        .medicineId(100L)
                        .medicineName("阿莫西林")
                        .pharmacyId(10L)
                        .quantity(2)
                        .unit("盒")
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/prescription/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("doctorId", "99")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.prescriptionSn").value("RX_20260629_001_000001"))
                .andExpect(jsonPath("$.data.items[0].medicineName").value("阿莫西林"));
    }

    @Test
    @DisplayName("POST /api/v1/prescription/issue - 库存不足返回400")
    void issuePrescription_insufficientStock_returns400() throws Exception {
        when(prescriptionService.issuePrescription(any(PrescriptionIssueRequest.class), anyLong()))
                .thenThrow(new BusinessException(ResultCode.STOCK_NOT_ENOUGH));

        PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                .patientId(1L)
                .diagnosis("感冒")
                .medicines(List.of(PrescriptionIssueRequest.MedicineItem.builder()
                        .medicineId(100L)
                        .medicineName("阿莫西林")
                        .pharmacyId(10L)
                        .quantity(999)
                        .unit("盒")
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/prescription/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("doctorId", "99")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // BusinessException 由 GlobalExceptionHandler 处理
                .andExpect(jsonPath("$.code").value(500)); // GlobalExceptionHandler 返回 FAIL code
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions - 查询患者处方列表")
    void listPrescriptions_success() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .patientId(1L)
                .diagnosis("感冒")
                .build();

        when(prescriptionService.listByPatient(1L)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/prescriptions")
                        .param("patientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].prescriptionSn").value("RX_20260629_001_000001"));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions - 空列表")
    void listPrescriptions_empty() throws Exception {
        when(prescriptionService.listByPatient(999L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/prescriptions")
                        .param("patientId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/{id} - 查询处方详情")
    void getPrescriptionDetail_success() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .patientId(1L)
                .diagnosis("感冒")
                .items(List.of(PrescriptionItemVO.builder()
                        .medicineName("阿莫西林")
                        .quantity(2)
                        .unit("盒")
                        .build()))
                .build();

        when(prescriptionService.getDetail(eq(1L), any())).thenReturn(vo);

        mockMvc.perform(get("/api/v1/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].medicineName").value("阿莫西林"));
    }

    @Test
    @DisplayName("GET /api/v1/pharmacy/inventory - 查询药房库存")
    void listInventory_success() throws Exception {
        InventoryVO inv = InventoryVO.builder()
                .pharmacyId(10L)
                .medicineName("阿莫西林")
                .stock(100)
                .lockStock(5)
                .unit("盒")
                .build();

        when(prescriptionService.listInventory(10L)).thenReturn(List.of(inv));

        mockMvc.perform(get("/api/v1/pharmacy/inventory")
                        .param("pharmacyId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stock").value(100))
                .andExpect(jsonPath("$.data[0].medicineName").value("阿莫西林"));
    }

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/audit - 审核通过")
    void auditPrescription_approve_returnsOk() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .patientId(1L)
                .auditStatus(1)
                .pharmacistId(50L)
                .auditComments("审核通过")
                .build();

        when(prescriptionService.auditPrescription(eq(1L), any(PrescriptionAuditRequest.class), eq(50L)))
                .thenReturn(vo);

        PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                .action("APPROVE")
                .comments("审核通过")
                .build();

        mockMvc.perform(post("/api/v1/prescriptions/1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("pharmacistId", "50")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.auditStatus").value(1))
                .andExpect(jsonPath("$.data.pharmacistId").value(50));
    }

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/audit - 审核驳回")
    void auditPrescription_reject_returnsOk() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .auditStatus(2)
                .pharmacistId(50L)
                .auditComments("剂量不合理")
                .build();

        when(prescriptionService.auditPrescription(eq(1L), any(PrescriptionAuditRequest.class), eq(50L)))
                .thenReturn(vo);

        PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                .action("REJECT")
                .comments("剂量不合理")
                .build();

        mockMvc.perform(post("/api/v1/prescriptions/1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("pharmacistId", "50")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.auditStatus").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/prescriptions/{id}/audit - 空action参数校验失败")
    void auditPrescription_invalidBody_returnsError() throws Exception {
        PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                .action("")
                .build();

        mockMvc.perform(post("/api/v1/prescriptions/1/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/pending-audit - 查询待审核列表")
    void listPendingAudit_returnsOk() throws Exception {
        PrescriptionVO vo = PrescriptionVO.builder()
                .id(1L)
                .prescriptionSn("RX_20260629_001_000001")
                .auditStatus(0)
                .build();

        when(prescriptionService.listPendingAudit()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/prescriptions/pending-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].auditStatus").value(0));
    }
}
