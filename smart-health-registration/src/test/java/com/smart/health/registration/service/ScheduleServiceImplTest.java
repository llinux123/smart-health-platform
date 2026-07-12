package com.smart.health.registration.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillOrderMessage;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.mapper.DoctorMapper;
import com.smart.health.registration.service.impl.ScheduleServiceImpl;
import com.smart.health.registration.util.OrderSnGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleServiceImpl 单元测试")
class ScheduleServiceImplTest {

    @Mock
    private DoctorScheduleMapper doctorScheduleMapper;

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private ScheduleRedisConfig scheduleRedisConfig;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OrderSnGenerator orderSnGenerator;

    @Mock
    private RLock rLock;

    private ScheduleServiceImpl scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleServiceImpl(
                doctorScheduleMapper,
                doctorMapper,
                scheduleRedisConfig,
                rabbitTemplate,
                orderSnGenerator
        );
    }

    @Test
    @DisplayName("创建排班 - 成功写穿透 Redis 库存与价格")
    void createSchedule_success() {
        // Given
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setDoctorId(1L);
        request.setDeptName("内科");
        request.setWorkDate(LocalDate.now().plusDays(1));
        request.setShift(1);
        request.setTotalCount(10);
        request.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.insert(any())).thenAnswer(invocation -> {
            DoctorSchedule schedule = invocation.getArgument(0);
            schedule.setId(100L);
            return 1;
        });

        // When
        scheduleService.createSchedule(request);

        // Then
        verify(doctorScheduleMapper).insert(any());
        verify(scheduleRedisConfig).initScheduleStock(100L, 10, new BigDecimal("50.00"));
        // 未传 departmentId 时应自动查询医生主科室
        verify(doctorMapper).selectPrimaryDepartmentId(1L);
    }

    @Test
    @DisplayName("创建排班 - 未传科室ID时自动补全主科室")
    void createSchedule_autoResolveDepartmentId() {
        // Given
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setDoctorId(5L);
        request.setDeptName("内科");
        request.setWorkDate(LocalDate.now().plusDays(1));
        request.setShift(1);
        request.setTotalCount(20);
        request.setPrice(new BigDecimal("30.00"));

        when(doctorMapper.selectPrimaryDepartmentId(5L)).thenReturn(1L);
        when(doctorScheduleMapper.insert(any())).thenAnswer(invocation -> {
            DoctorSchedule schedule = invocation.getArgument(0);
            schedule.setId(200L);
            return 1;
        });

        // When
        scheduleService.createSchedule(request);

        // Then
        verify(doctorMapper).selectPrimaryDepartmentId(5L);
        verify(scheduleRedisConfig).initScheduleStock(200L, 20, new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("创建排班 - 已传科室ID时不查询医生主科室")
    void createSchedule_withDepartmentId_skipAutoResolve() {
        // Given
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setDoctorId(1L);
        request.setDepartmentId(3L);
        request.setDeptName("骨科");
        request.setWorkDate(LocalDate.now().plusDays(1));
        request.setShift(2);
        request.setTotalCount(15);
        request.setPrice(new BigDecimal("80.00"));

        when(doctorScheduleMapper.insert(any())).thenAnswer(invocation -> {
            DoctorSchedule schedule = invocation.getArgument(0);
            schedule.setId(300L);
            return 1;
        });

        // When
        scheduleService.createSchedule(request);

        // Then
        verify(doctorMapper, never()).selectPrimaryDepartmentId(anyLong());
        verify(scheduleRedisConfig).initScheduleStock(300L, 15, new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("秒杀抢号 - Redis 中无库存缓存视为排班不存在")
    void seckill_scheduleNotFound_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(999L);
        Long patientId = 1L;

        when(scheduleRedisConfig.getScheduleStock(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SCHEDULE_NOT_FOUND.getCode());

        verify(scheduleRedisConfig, never()).isPatientInSeckillSet(anyLong(), anyLong());
        verify(doctorScheduleMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 价格缓存缺失视为排班不存在")
    void seckill_priceMissing_throwsException() {
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(null);

        assertThatThrownBy(() -> scheduleService.seckill(request, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SCHEDULE_NOT_FOUND.getCode());

        verify(scheduleRedisConfig, never()).decrementStock(anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 重复抢号抛出异常")
    void seckill_repeatSeckill_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.REPEAT_SECKILL.getCode());

        verify(scheduleRedisConfig, never()).tryAcquireSeckillLock(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 获取锁失败抛出异常")
    void seckill_acquireLockFails_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SECKILL_FAIL.getCode());

        verify(scheduleRedisConfig, never()).decrementStock(anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 库存不足抛出异常并回滚")
    void seckill_stockEmpty_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(scheduleRedisConfig.decrementStock(1L)).thenReturn(-1L);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.STOCK_EMPTY.getCode());

        verify(scheduleRedisConfig).incrementStock(1L);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("秒杀抢号 - MQ 发送失败回滚库存与幂等集合")
    void seckill_mqSendFails_rollbackStock() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(scheduleRedisConfig.decrementStock(1L)).thenReturn(5L);
        when(orderSnGenerator.generate()).thenReturn("REG_20260629_000001");
        org.mockito.Mockito.doThrow(new AmqpException("rabbit down"))
                .when(rabbitTemplate).convertAndSend(
                        eq("exchange.registration"),
                        eq("order.create"),
                        any(SeckillOrderMessage.class));

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SECKILL_FAIL.getCode());

        verify(scheduleRedisConfig).incrementStock(1L);
        verify(scheduleRedisConfig).removePatientFromSeckillSet(1L, 100L);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("秒杀抢号 - 成功场景使用 Jackson Converter 发送对象")
    void seckill_success() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        when(scheduleRedisConfig.getScheduleStock(1L)).thenReturn(10L);
        when(scheduleRedisConfig.getSchedulePrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(scheduleRedisConfig.decrementStock(1L)).thenReturn(5L);
        when(orderSnGenerator.generate()).thenReturn("REG_20260629_000001");

        // When
        SeckillResponse response = scheduleService.seckill(request, patientId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderSn()).startsWith("REG_");
        assertThat(response.getStatus()).isEqualTo("QUEUING");

        verify(scheduleRedisConfig).addPatientToSeckillSet(1L, 100L);
        verify(rabbitTemplate).convertAndSend(
                eq("exchange.registration"),
                eq("order.create"),
                any(com.smart.health.registration.dto.SeckillOrderMessage.class)
        );
        verify(rLock).unlock();
        // 关键：热路径不再访问 DB
        verify(doctorScheduleMapper, never()).selectById(anyLong());
    }
}
