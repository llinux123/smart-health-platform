package com.smart.health.prescription.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.prescription.dto.InventoryVO;
import com.smart.health.prescription.dto.PrescriptionAuditRequest;
import com.smart.health.prescription.dto.PrescriptionIssueRequest;
import com.smart.health.prescription.dto.PrescriptionVO;
import com.smart.health.prescription.entity.PharmacyInventory;
import com.smart.health.prescription.entity.Prescription;
import com.smart.health.prescription.entity.PrescriptionItem;
import com.smart.health.prescription.enums.AuditStatus;
import com.smart.health.prescription.enums.PrescriptionStatus;
import com.smart.health.prescription.mapper.PharmacyInventoryMapper;
import com.smart.health.prescription.mapper.PrescriptionItemMapper;
import com.smart.health.prescription.mapper.PrescriptionMapper;
import com.smart.health.prescription.service.impl.PrescriptionServiceImpl;
import com.smart.health.prescription.util.PrescriptionCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionServiceImpl 单元测试")
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private PharmacyInventoryMapper pharmacyInventoryMapper;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @Mock
    private PrescriptionCodeGenerator prescriptionCodeGenerator;

    private PrescriptionServiceImpl prescriptionService;

    @BeforeEach
    void setUp() {
        prescriptionService = new PrescriptionServiceImpl(
                prescriptionMapper, prescriptionItemMapper,
                pharmacyInventoryMapper, pdfGenerationService,
                prescriptionCodeGenerator
        );
    }

    @Nested
    @DisplayName("issuePrescription - 开具处方")
    class IssuePrescription {

        @Test
        @DisplayName("库存充足时成功开具处方并扣减库存")
        void issuePrescription_sufficientStock_success() {
            // given
            PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                    .patientId(1L)
                    .diagnosis("感冒")
                    .medicines(List.of(
                            PrescriptionIssueRequest.MedicineItem.builder()
                                    .medicineId(100L)
                                    .medicineName("阿莫西林")
                                    .pharmacyId(10L)
                                    .quantity(2)
                                    .unit("盒")
                                    .build()
                    ))
                    .build();

            PharmacyInventory inventory = new PharmacyInventory();
            inventory.setPharmacyId(10L);
            inventory.setMedicineId(100L);
            inventory.setStock(50);

            when(pharmacyInventoryMapper.selectByPharmacyAndMedicine(10L, 100L))
                    .thenReturn(inventory);
            when(prescriptionMapper.insert(any(Prescription.class))).thenReturn(1);
            when(pharmacyInventoryMapper.deductStock(10L, 100L, 2)).thenReturn(1);
            when(prescriptionItemMapper.batchInsert(anyList())).thenReturn(1);
            when(pdfGenerationService.generatePrescriptionPdf(any(), any(), any(), any(), any()))
                    .thenReturn("/tmp/rx_test.pdf");
            when(prescriptionMapper.updatePdfUrl(any(), any())).thenReturn(1);
            when(prescriptionCodeGenerator.generate()).thenReturn("RX_20260629_001_000001");

            // when
            PrescriptionVO result = prescriptionService.issuePrescription(request, 99L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getPatientId()).isEqualTo(1L);
            assertThat(result.getDoctorId()).isEqualTo(99L);
            assertThat(result.getDiagnosis()).isEqualTo("感冒");
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getMedicineName()).isEqualTo("阿莫西林");

            verify(pharmacyInventoryMapper).deductStock(10L, 100L, 2);
            verify(prescriptionItemMapper).batchInsert(anyList());
        }

        @Test
        @DisplayName("库存不足时抛出 STOCK_NOT_ENOUGH 异常")
        void issuePrescription_insufficientStock_throws() {
            // given
            PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                    .patientId(1L)
                    .diagnosis("感冒")
                    .medicines(List.of(
                            PrescriptionIssueRequest.MedicineItem.builder()
                                    .medicineId(100L)
                                    .medicineName("阿莫西林")
                                    .pharmacyId(10L)
                                    .quantity(100)
                                    .unit("盒")
                                    .build()
                    ))
                    .build();

            PharmacyInventory inventory = new PharmacyInventory();
            inventory.setStock(5); // 只有5个，需要100个

            when(pharmacyInventoryMapper.selectByPharmacyAndMedicine(10L, 100L))
                    .thenReturn(inventory);

            // when & then
            assertThatThrownBy(() -> prescriptionService.issuePrescription(request, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");

            verify(prescriptionMapper, never()).insert(any());
        }

        @Test
        @DisplayName("库存记录不存在时抛出异常")
        void issuePrescription_noInventoryRecord_throws() {
            PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                    .patientId(1L)
                    .diagnosis("感冒")
                    .medicines(List.of(
                            PrescriptionIssueRequest.MedicineItem.builder()
                                    .medicineId(999L)
                                    .medicineName("不存在的药")
                                    .pharmacyId(10L)
                                    .quantity(1)
                                    .unit("盒")
                                    .build()
                    ))
                    .build();

            when(pharmacyInventoryMapper.selectByPharmacyAndMedicine(10L, 999L))
                    .thenReturn(null);

            assertThatThrownBy(() -> prescriptionService.issuePrescription(request, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");
        }

        @Test
        @DisplayName("deductStock 返回0时（并发库存不足）抛出异常")
        void issuePrescription_deductStockReturnsZero_throws() {
            PrescriptionIssueRequest request = PrescriptionIssueRequest.builder()
                    .patientId(1L)
                    .diagnosis("感冒")
                    .medicines(List.of(
                            PrescriptionIssueRequest.MedicineItem.builder()
                                    .medicineId(100L)
                                    .medicineName("阿莫西林")
                                    .pharmacyId(10L)
                                    .quantity(2)
                                    .unit("盒")
                                    .build()
                    ))
                    .build();

            // 预检通过
            PharmacyInventory inventory = new PharmacyInventory();
            inventory.setStock(50);
            when(pharmacyInventoryMapper.selectByPharmacyAndMedicine(10L, 100L))
                    .thenReturn(inventory);
            when(prescriptionMapper.insert(any(Prescription.class))).thenReturn(1);
            // 但实际扣减时库存不足（并发场景）
            when(pharmacyInventoryMapper.deductStock(10L, 100L, 2)).thenReturn(0);

            assertThatThrownBy(() -> prescriptionService.issuePrescription(request, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("库存不足");
        }
    }

    @Nested
    @DisplayName("listByPatient - 查询患者处方列表")
    class ListByPatient {

        @Test
        @DisplayName("返回患者处方列表（不含药品明细）")
        void listByPatient_returnsList() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPrescriptionSn("RX_20260629_001_000001");
            p.setPatientId(1L);
            p.setDoctorId(99L);
            p.setDiagnosis("感冒");
            p.setAuditStatus(AuditStatus.PENDING);
            p.setStatus(PrescriptionStatus.NOT_DISPENSED);
            p.setCreateTime(LocalDateTime.now());

            when(prescriptionMapper.selectByPatientId(1L)).thenReturn(List.of(p));

            List<PrescriptionVO> result = prescriptionService.listByPatient(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPrescriptionSn()).isEqualTo("RX_20260629_001_000001");
            assertThat(result.get(0).getItems()).isNull(); // 列表不含明细
        }

        @Test
        @DisplayName("无处方时返回空列表")
        void listByPatient_empty_returnsEmptyList() {
            when(prescriptionMapper.selectByPatientId(999L)).thenReturn(Collections.emptyList());

            List<PrescriptionVO> result = prescriptionService.listByPatient(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDetail - 查询处方详情")
    class GetDetail {

        @Test
        @DisplayName("返回处方详情含药品明细")
        void getDetail_existing_returnsWithItems() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPrescriptionSn("RX_20260629_001_000001");
            p.setPatientId(1L);
            p.setDoctorId(99L);
            p.setDiagnosis("感冒");
            p.setAuditStatus(AuditStatus.PENDING);
            p.setStatus(PrescriptionStatus.NOT_DISPENSED);

            PrescriptionItem item = new PrescriptionItem();
            item.setMedicineName("阿莫西林");
            item.setQuantity(2);
            item.setUnit("盒");

            when(prescriptionMapper.selectById(1L)).thenReturn(p);
            when(prescriptionItemMapper.selectByPrescriptionId(1L)).thenReturn(List.of(item));

            PrescriptionVO result = prescriptionService.getDetail(1L, 1L);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getMedicineName()).isEqualTo("阿莫西林");
        }

        @Test
        @DisplayName("处方不存在时抛出异常")
        void getDetail_notFound_throws() {
            when(prescriptionMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> prescriptionService.getDetail(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("处方不存在");
        }

        @Test
        @DisplayName("患者ID不匹配时抛出 FORBIDDEN 异常")
        void getDetail_wrongPatient_throws() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPatientId(1L);

            when(prescriptionMapper.selectById(1L)).thenReturn(p);

            assertThatThrownBy(() -> prescriptionService.getDetail(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权查看");
        }
    }

    @Nested
    @DisplayName("listInventory - 查询药房库存")
    class ListInventory {

        @Test
        @DisplayName("返回药房库存列表")
        void listInventory_returnsList() {
            PharmacyInventory inv = new PharmacyInventory();
            inv.setPharmacyId(10L);
            inv.setMedicineName("阿莫西林");
            inv.setStock(100);
            inv.setLockStock(5);
            inv.setUnit("盒");

            when(pharmacyInventoryMapper.selectByPharmacyId(10L)).thenReturn(List.of(inv));

            List<InventoryVO> result = prescriptionService.listInventory(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStock()).isEqualTo(100);
            assertThat(result.get(0).getMedicineName()).isEqualTo("阿莫西林");
        }
    }

    @Nested
    @DisplayName("auditPrescription - 审核处方")
    class AuditPrescription {

        @Test
        @DisplayName("审核通过: 状态更新为1，不恢复库存")
        void auditPrescription_approve_success() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPrescriptionSn("RX_20260629_001_000001");
            p.setPatientId(1L);
            p.setDoctorId(99L);
            p.setDiagnosis("感冒");
            p.setAuditStatus(AuditStatus.PENDING);
            p.setStatus(PrescriptionStatus.NOT_DISPENSED);

            PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                    .action("APPROVE")
                    .comments("审核通过")
                    .build();

            when(prescriptionMapper.selectById(1L)).thenReturn(p);
            when(prescriptionMapper.updateAuditStatus(eq(1L), eq(AuditStatus.APPROVED), eq(50L), eq("审核通过"), any()))
                    .thenReturn(1);

            PrescriptionVO result = prescriptionService.auditPrescription(1L, request, 50L);

            assertThat(result).isNotNull();
            assertThat(result.getAuditStatus()).isEqualTo(AuditStatus.APPROVED);
            assertThat(result.getPharmacistId()).isEqualTo(50L);
            assertThat(result.getAuditComments()).isEqualTo("审核通过");
            verify(pharmacyInventoryMapper, never()).restoreStock(anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("审核驳回: 状态更新为2，恢复库存")
        void auditPrescription_reject_restoresStock() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPrescriptionSn("RX_20260629_001_000001");
            p.setPatientId(1L);
            p.setDoctorId(99L);
            p.setDiagnosis("感冒");
            p.setAuditStatus(AuditStatus.PENDING);
            p.setStatus(PrescriptionStatus.NOT_DISPENSED);

            PrescriptionItem item1 = new PrescriptionItem();
            item1.setPrescriptionId(1L);
            item1.setMedicineId(100L);
            item1.setMedicineName("阿莫西林");
            item1.setPharmacyId(10L);
            item1.setQuantity(2);

            PrescriptionItem item2 = new PrescriptionItem();
            item2.setPrescriptionId(1L);
            item2.setMedicineId(200L);
            item2.setMedicineName("布洛芬");
            item2.setPharmacyId(10L);
            item2.setQuantity(1);

            PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                    .action("REJECT")
                    .comments("剂量不合理")
                    .build();

            when(prescriptionMapper.selectById(1L)).thenReturn(p);
            when(prescriptionMapper.updateAuditStatus(eq(1L), eq(AuditStatus.REJECTED), eq(50L), eq("剂量不合理"), any()))
                    .thenReturn(1);
            when(prescriptionItemMapper.selectByPrescriptionId(1L)).thenReturn(List.of(item1, item2));
            when(pharmacyInventoryMapper.restoreStock(10L, 100L, 2)).thenReturn(1);
            when(pharmacyInventoryMapper.restoreStock(10L, 200L, 1)).thenReturn(1);

            PrescriptionVO result = prescriptionService.auditPrescription(1L, request, 50L);

            assertThat(result).isNotNull();
            assertThat(result.getAuditStatus()).isEqualTo(AuditStatus.REJECTED);
            assertThat(result.getAuditComments()).isEqualTo("剂量不合理");
            verify(pharmacyInventoryMapper).restoreStock(10L, 100L, 2);
            verify(pharmacyInventoryMapper).restoreStock(10L, 200L, 1);
        }

        @Test
        @DisplayName("处方不存在时抛出 PRESCRIPTION_NOT_FOUND")
        void auditPrescription_notFound_throwsException() {
            when(prescriptionMapper.selectById(999L)).thenReturn(null);

            PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                    .action("APPROVE")
                    .build();

            assertThatThrownBy(() -> prescriptionService.auditPrescription(999L, request, 50L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("处方不存在");
        }

        @Test
        @DisplayName("重复审核时抛出 PRESCRIPTION_ALREADY_AUDITED")
        void auditPrescription_alreadyAudited_throwsException() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setAuditStatus(AuditStatus.APPROVED); // 已通过

            when(prescriptionMapper.selectById(1L)).thenReturn(p);

            PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                    .action("REJECT")
                    .build();

            assertThatThrownBy(() -> prescriptionService.auditPrescription(1L, request, 50L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("处方已审核");
        }

        @Test
        @DisplayName("非法审核动作抛出 PARAM_ERROR")
        void auditPrescription_invalidAction_throwsException() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setAuditStatus(AuditStatus.PENDING);

            when(prescriptionMapper.selectById(1L)).thenReturn(p);

            PrescriptionAuditRequest request = PrescriptionAuditRequest.builder()
                    .action("UNKNOWN")
                    .build();

            assertThatThrownBy(() -> prescriptionService.auditPrescription(1L, request, 50L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的审核动作");
        }
    }

    @Nested
    @DisplayName("listPendingAudit - 查询待审核处方")
    class ListPendingAudit {

        @Test
        @DisplayName("返回待审核处方列表")
        void listPendingAudit_returnsList() {
            Prescription p = new Prescription();
            p.setId(1L);
            p.setPrescriptionSn("RX_20260629_001_000001");
            p.setPatientId(1L);
            p.setAuditStatus(AuditStatus.PENDING);

            when(prescriptionMapper.selectByAuditStatus(AuditStatus.PENDING)).thenReturn(List.of(p));

            List<PrescriptionVO> result = prescriptionService.listPendingAudit();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAuditStatus()).isEqualTo(AuditStatus.PENDING);
            verify(prescriptionMapper).selectByAuditStatus(AuditStatus.PENDING);
        }

        @Test
        @DisplayName("无待审核处方时返回空列表")
        void listPendingAudit_empty() {
            when(prescriptionMapper.selectByAuditStatus(AuditStatus.PENDING)).thenReturn(Collections.emptyList());

            List<PrescriptionVO> result = prescriptionService.listPendingAudit();

            assertThat(result).isEmpty();
        }
    }
}
