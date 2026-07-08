package com.smart.health.registration.service;

import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.mapper.RegistrationOrderMapper;
import com.smart.health.registration.service.impl.RegistrationOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationOrderServiceImpl 单元测试")
class RegistrationOrderServiceImplTest {

    @Mock
    private RegistrationOrderMapper registrationOrderMapper;

    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;

    @Mock
    private ScheduleRedisConfig scheduleRedisConfig;

    private RegistrationOrderServiceImpl registrationOrderService;

    @BeforeEach
    void setUp() {
        registrationOrderService = new RegistrationOrderServiceImpl(
                registrationOrderMapper,
                doctorScheduleMapper,
                scheduleRedisConfig
        );
    }

    @Test
    @DisplayName("创建订单 - 成功并同步 DB 库存")
    void createOrder_success_syncsDbInventory() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setAmount(new BigDecimal("50.00"));

        when(registrationOrderMapper.selectMaxSequenceNumber(1L)).thenReturn(0);
        when(registrationOrderMapper.insert(any())).thenReturn(1);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setVersion(3);
        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(doctorScheduleMapper.decrementVisibleCount(1L, 3)).thenReturn(1);

        // When
        RegistrationOrder result = registrationOrderService.createOrder(order);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(0);
        verify(registrationOrderMapper).insert(order);
        verify(doctorScheduleMapper).decrementVisibleCount(1L, 3);
    }

    @Test
    @DisplayName("创建订单 - 乐观锁冲突仅记录警告不抛异常")
    void createOrder_optimisticLockConflict_noException() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000002");
        order.setPatientId(101L);
        order.setScheduleId(1L);
        order.setAmount(new BigDecimal("50.00"));

        when(registrationOrderMapper.selectMaxSequenceNumber(1L)).thenReturn(5);
        when(registrationOrderMapper.insert(any())).thenReturn(1);

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setVersion(2);
        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(doctorScheduleMapper.decrementVisibleCount(1L, 2)).thenReturn(0); // 乐观锁冲突

        // When
        RegistrationOrder result = registrationOrderService.createOrder(order);

        // Then - 订单仍然创建成功，DB 库存同步失败仅记录日志
        assertThat(result).isNotNull();
        assertThat(result.getSequenceNumber()).isEqualTo(6);
        verify(registrationOrderMapper).insert(order);
        verify(doctorScheduleMapper).decrementVisibleCount(1L, 2);
    }

    @Test
    @DisplayName("创建订单 - 排班不存在仅记录警告")
    void createOrder_scheduleNotFound_logWarning() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000003");
        order.setPatientId(102L);
        order.setScheduleId(999L);
        order.setAmount(new BigDecimal("50.00"));

        when(registrationOrderMapper.selectMaxSequenceNumber(999L)).thenReturn(0);
        when(registrationOrderMapper.insert(any())).thenReturn(1);
        when(doctorScheduleMapper.selectById(999L)).thenReturn(null);

        // When
        RegistrationOrder result = registrationOrderService.createOrder(order);

        // Then
        assertThat(result).isNotNull();
        verify(doctorScheduleMapper, never()).decrementVisibleCount(eq(999L), any());
    }

    @Test
    @DisplayName("根据订单号查询订单")
    void getByOrderSn_returnsOrder() {
        // Given
        RegistrationOrder expected = new RegistrationOrder();
        expected.setOrderSn("REG_20260629_000001");
        when(registrationOrderMapper.selectByOrderSn("REG_20260629_000001")).thenReturn(expected);

        // When
        RegistrationOrder result = registrationOrderService.getByOrderSn("REG_20260629_000001");

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("查询患者订单列表")
    void listByPatientId_returnsList() {
        // Given
        when(registrationOrderMapper.selectByPatientId(100L)).thenReturn(List.of(new RegistrationOrder()));

        // When
        List<RegistrationOrder> result = registrationOrderService.listByPatientId(100L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("取消订单 - 恢复 Redis 库存 + DB 号源 + 移除幂等集合")
    void cancelOrder_restoresInventoryAndRemovesFromSeckillSet() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setStatus(1); // 待支付

        when(registrationOrderMapper.selectByOrderSn("REG_20260629_000001")).thenReturn(order);
        when(registrationOrderMapper.updateStatus("REG_20260629_000001", 4, List.of(0, 1))).thenReturn(1);
        when(doctorScheduleMapper.incrementVisibleCountUnchecked(1L)).thenReturn(1);

        // When
        registrationOrderService.cancelOrder("REG_20260629_000001", 100L);

        // Then
        verify(scheduleRedisConfig).incrementStock(1L);
        verify(doctorScheduleMapper).incrementVisibleCountUnchecked(1L);
        verify(doctorScheduleMapper, never()).incrementVisibleCount(anyLong(), anyInt());
        verify(scheduleRedisConfig).removePatientFromSeckillSet(1L, 100L);
    }

    @Test
    @DisplayName("取消订单 - 已是已退号状态幂等返回")
    void cancelOrder_alreadyCancelled_idempotent() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setStatus(4); // 已退号

        when(registrationOrderMapper.selectByOrderSn("REG_20260629_000001")).thenReturn(order);

        // When
        registrationOrderService.cancelOrder("REG_20260629_000001", 100L);

        // Then - 不应调用恢复库存
        verify(scheduleRedisConfig, never()).incrementStock(any());
        verify(scheduleRedisConfig, never()).removePatientFromSeckillSet(any(), any());
    }

    @Test
    @DisplayName("取消订单 - patientId 不匹配时抛出 BusinessException")
    void cancelOrder_patientIdMismatch_throwsException() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setStatus(1); // 待支付

        when(registrationOrderMapper.selectByOrderSn("REG_20260629_000001")).thenReturn(order);

        // When & Then — 用 999L（非订单所有者 100L）调用
        assertThatThrownBy(() -> registrationOrderService.cancelOrder("REG_20260629_000001", 999L))
                .isInstanceOf(com.smart.health.common.exception.BusinessException.class)
                .hasMessageContaining("无权操作");

        verify(registrationOrderMapper, never()).updateStatus(anyString(), anyInt(), anyList());
        verify(scheduleRedisConfig, never()).incrementStock(any());
    }

    @Test
    @DisplayName("支付订单 - patientId 不匹配时抛出 BusinessException")
    void payOrder_patientIdMismatch_throwsException() {
        // Given
        RegistrationOrder order = new RegistrationOrder();
        order.setOrderSn("REG_20260629_000001");
        order.setPatientId(100L);
        order.setScheduleId(1L);
        order.setStatus(1); // 待支付

        when(registrationOrderMapper.selectByOrderSn("REG_20260629_000001")).thenReturn(order);

        // When & Then — 用 999L（非订单所有者 100L）调用
        assertThatThrownBy(() -> registrationOrderService.payOrder("REG_20260629_000001", 999L))
                .isInstanceOf(com.smart.health.common.exception.BusinessException.class)
                .hasMessageContaining("无权操作");

        verify(registrationOrderMapper, never()).updateStatusWithPayTime(anyString(), anyInt(), anyList(), any());
    }

    @Test
    @DisplayName("查询订单详情 - 患者查看他人订单抛出 BusinessException")
    void getOrderVOByOrderSn_patientIdMismatch_throwsException() {
        // Given — 单条 SQL 按订单号+患者ID查询，归属不符返回 null
        when(registrationOrderMapper.selectOrderVOByOrderSnAndPatientId("REG_20260629_000001", 999L))
                .thenReturn(null);

        // When & Then — 患者 999L 查看患者 100L 的订单
        assertThatThrownBy(() -> registrationOrderService.getOrderVOByOrderSn("REG_20260629_000001", 999L, "PATIENT"))
                .isInstanceOf(com.smart.health.common.exception.BusinessException.class)
                .hasMessageContaining("订单不存在");

        // 不应触发归属不符时的旧分支（不应再调用 selectByOrderSn + selectOrderVOByOrderSn 两条）
        verify(registrationOrderMapper, never()).selectByOrderSn(anyString());
        verify(registrationOrderMapper, never()).selectOrderVOByOrderSn(anyString());
    }

    @Test
    @DisplayName("查询订单详情 - PATIENT 命中自身订单时单条 SQL 返回 VO")
    void getOrderVOByOrderSn_patientMatch_returnsOrder() {
        // Given
        OrderVO expected = new OrderVO();
        expected.setOrderSn("REG_20260629_000001");
        expected.setPatientId(100L);

        when(registrationOrderMapper.selectOrderVOByOrderSnAndPatientId("REG_20260629_000001", 100L))
                .thenReturn(expected);

        // When
        OrderVO result = registrationOrderService.getOrderVOByOrderSn("REG_20260629_000001", 100L, "PATIENT");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderSn()).isEqualTo("REG_20260629_000001");
        verify(registrationOrderMapper).selectOrderVOByOrderSnAndPatientId("REG_20260629_000001", 100L);
        verify(registrationOrderMapper, never()).selectOrderVOByOrderSn(anyString());
    }

    @Test
    @DisplayName("查询订单详情 - 管理员路径不存在时返回 ORDER_NOT_FOUND")
    void getOrderVOByOrderSn_adminNotFound_throwsException() {
        // Given
        when(registrationOrderMapper.selectOrderVOByOrderSn("REG_20260629_000001")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> registrationOrderService.getOrderVOByOrderSn("REG_20260629_000001", 999L, "ADMIN"))
                .isInstanceOf(com.smart.health.common.exception.BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @DisplayName("分页查询订单 - 默认每页大小与偏移计算正确")
    void pageOrderVOByPatientId_defaultsAndOffset() {
        // Given
        when(registrationOrderMapper.countByPatientId(100L)).thenReturn(45);
        when(registrationOrderMapper.selectOrderVOByPatientIdPaged(100L, 0, 20))
                .thenReturn(List.of(new OrderVO()));

        // When - 默认 page=1, size=20 → offset=0
        com.smart.health.common.result.PageResult<OrderVO> page1 =
                registrationOrderService.pageOrderVOByPatientId(100L, 1, 20);

        // Then
        assertThat(page1.getTotal()).isEqualTo(45L);
        assertThat(page1.getPage()).isEqualTo(1);
        assertThat(page1.getSize()).isEqualTo(20);
        assertThat(page1.getList()).hasSize(1);
        verify(registrationOrderMapper).selectOrderVOByPatientIdPaged(100L, 0, 20);
    }

    @Test
    @DisplayName("分页查询订单 - 超出上限 size 被裁剪到 100，非法 page/size 走默认值")
    void pageOrderVOByPatientId_clampsSize() {
        // Given
        when(registrationOrderMapper.countByPatientId(100L)).thenReturn(0);
        when(registrationOrderMapper.selectOrderVOByPatientIdPaged(eq(100L), eq(0), eq(100)))
                .thenReturn(List.of());

        // When - size=500 → 裁剪到 100；page=-5 → 走 1
        com.smart.health.common.result.PageResult<OrderVO> result =
                registrationOrderService.pageOrderVOByPatientId(100L, -5, 500);

        // Then
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(100);
        verify(registrationOrderMapper).selectOrderVOByPatientIdPaged(100L, 0, 100);
    }

    @Test
    @DisplayName("查询订单详情 - 管理员查看任意订单不校验归属")
    void getOrderVOByOrderSn_adminNoOwnershipCheck_returnsOrder() {
        // Given
        OrderVO expected = new OrderVO();
        expected.setOrderSn("REG_20260629_000001");
        expected.setPatientId(100L);

        when(registrationOrderMapper.selectOrderVOByOrderSn("REG_20260629_000001")).thenReturn(expected);

        // When — 管理员查看任意患者的订单
        OrderVO result = registrationOrderService.getOrderVOByOrderSn("REG_20260629_000001", 999L, "ADMIN");

        // Then — 不校验归属，直接返回
        assertThat(result).isNotNull();
        assertThat(result.getOrderSn()).isEqualTo("REG_20260629_000001");
    }
}
