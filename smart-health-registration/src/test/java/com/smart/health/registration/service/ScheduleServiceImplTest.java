package com.smart.health.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.service.impl.ScheduleServiceImpl;
import com.smart.health.registration.util.OrderSnGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private ScheduleRedisConfig scheduleRedisConfig;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderSnGenerator orderSnGenerator;

    @Mock
    private RLock rLock;

    private ScheduleServiceImpl scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleServiceImpl(
                doctorScheduleMapper,
                scheduleRedisConfig,
                rabbitTemplate,
                objectMapper,
                orderSnGenerator
        );
    }

    @Test
    @DisplayName("创建排班 - 成功初始化 Redis 库存")
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
        verify(scheduleRedisConfig).initScheduleStock(100L, 10);
    }

    @Test
    @DisplayName("秒杀抢号 - 排班不存在抛出异常")
    void seckill_scheduleNotFound_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(999L);
        Long patientId = 1L;

        when(doctorScheduleMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SCHEDULE_NOT_FOUND.getCode());

        verify(scheduleRedisConfig, never()).isPatientInSeckillSet(anyLong(), anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 重复抢号抛出异常")
    void seckill_repeatSeckill_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
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

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SECKILL_FAIL.getCode());

        verify(scheduleRedisConfig, never()).decrementStock(anyLong());
    }

    @Test
    @DisplayName("秒杀抢号 - 库存不足抛出异常")
    void seckill_stockEmpty_throwsException() {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
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
    @DisplayName("秒杀抢号 - MQ 发送失败回滚库存")
    void seckill_mqSendFails_rollbackStock() throws Exception {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(scheduleRedisConfig.decrementStock(1L)).thenReturn(5L);
        when(orderSnGenerator.generate()).thenReturn("REG_20260629_000001");
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        // When & Then
        assertThatThrownBy(() -> scheduleService.seckill(request, patientId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.SECKILL_FAIL.getCode());

        verify(scheduleRedisConfig).incrementStock(1L);
        verify(scheduleRedisConfig).removePatientFromSeckillSet(1L, 100L);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("秒杀抢号 - 成功场景")
    void seckill_success() throws Exception {
        // Given
        SeckillRequest request = new SeckillRequest();
        request.setScheduleId(1L);
        Long patientId = 100L;

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(1L);
        schedule.setPrice(new BigDecimal("50.00"));

        when(doctorScheduleMapper.selectById(1L)).thenReturn(schedule);
        when(scheduleRedisConfig.isPatientInSeckillSet(1L, 100L)).thenReturn(false);
        when(scheduleRedisConfig.tryAcquireSeckillLock(1L, 100L, 3L, 10L)).thenReturn(rLock);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(scheduleRedisConfig.decrementStock(1L)).thenReturn(5L);
        when(orderSnGenerator.generate()).thenReturn("REG_20260629_000001");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderSn\":\"REG_20260629_000001\"}");

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
                anyString()
        );
        verify(rLock).unlock();
    }
}
