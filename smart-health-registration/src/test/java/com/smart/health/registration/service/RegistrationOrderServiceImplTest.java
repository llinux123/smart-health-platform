package com.smart.health.registration.service;

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
import static org.mockito.ArgumentMatchers.any;
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

    private RegistrationOrderServiceImpl registrationOrderService;

    @BeforeEach
    void setUp() {
        registrationOrderService = new RegistrationOrderServiceImpl(
                registrationOrderMapper,
                doctorScheduleMapper
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
}
