package com.smart.health.registration.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.ScheduleVO;
import com.smart.health.registration.dto.SeckillOrderMessage;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.service.ScheduleService;
import com.smart.health.registration.util.OrderSnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排班管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final DoctorScheduleMapper doctorScheduleMapper;
    private final ScheduleRedisConfig scheduleRedisConfig;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OrderSnGenerator orderSnGenerator;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSchedule(ScheduleCreateRequest request) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(request.getDoctorId());
        schedule.setDeptName(request.getDeptName());
        schedule.setWorkDate(request.getWorkDate());
        schedule.setShift(request.getShift());
        schedule.setTotalCount(request.getTotalCount());
        schedule.setVisibleCount(request.getTotalCount());
        schedule.setPrice(request.getPrice());

        doctorScheduleMapper.insert(schedule);

        // 初始化 Redis 库存
        scheduleRedisConfig.initScheduleStock(schedule.getId(), request.getTotalCount());

        log.info("创建排班成功，scheduleId={}, stock={}", schedule.getId(), request.getTotalCount());
    }

    @Override
    public List<ScheduleVO> getAvailableSchedules(String deptName, LocalDate workDate) {
        List<DoctorSchedule> list = doctorScheduleMapper.selectAvailableList(deptName, workDate);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SeckillResponse seckill(SeckillRequest request) {
        Long scheduleId = request.getScheduleId();
        Long patientId = request.getPatientId();

        // 1. 校验排班是否存在
        DoctorSchedule schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }

        // 2. 幂等性检查：是否已抢过
        if (scheduleRedisConfig.isPatientInSeckillSet(scheduleId, patientId)) {
            throw new BusinessException(ResultCode.REPEAT_SECKILL);
        }

        // 3. 获取分布式锁
        RLock lock = scheduleRedisConfig.tryAcquireSeckillLock(scheduleId, patientId, 3, 10);
        if (lock == null) {
            throw new BusinessException(ResultCode.SECKILL_FAIL);
        }

        try {
            // 4. 再次检查幂等性（双重检查）
            if (scheduleRedisConfig.isPatientInSeckillSet(scheduleId, patientId)) {
                throw new BusinessException(ResultCode.REPEAT_SECKILL);
            }

            // 5. Redis 原子预扣库存
            Long remain = scheduleRedisConfig.decrementStock(scheduleId);
            if (remain == null || remain < 0) {
                // 回滚库存
                if (remain != null && remain < 0) {
                    scheduleRedisConfig.incrementStock(scheduleId);
                }
                throw new BusinessException(ResultCode.STOCK_EMPTY);
            }

            // 6. 生成订单号
            String orderSn = orderSnGenerator.generate();

            // 7. 标记患者已抢（幂等性）
            scheduleRedisConfig.addPatientToSeckillSet(scheduleId, patientId);

            // 8. 发送 MQ 消息异步创建订单
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setOrderSn(orderSn);
            message.setPatientId(patientId);
            message.setScheduleId(scheduleId);
            message.setAmount(schedule.getPrice());

            try {
                rabbitTemplate.convertAndSend(
                        CommonConstants.MQ_EXCHANGE_REGISTRATION,
                        CommonConstants.MQ_ROUTING_KEY_ORDER_CREATE,
                        objectMapper.writeValueAsString(message)
                );
                log.info("秒杀订单消息已发送，orderSn={}, scheduleId={}, patientId={}", orderSn, scheduleId, patientId);
            } catch (Exception e) {
                // MQ 发送失败，回滚
                log.error("MQ消息发送失败，回滚库存，orderSn={}", orderSn, e);
                scheduleRedisConfig.incrementStock(scheduleId);
                scheduleRedisConfig.removePatientFromSeckillSet(scheduleId, patientId);
                throw new BusinessException(ResultCode.SECKILL_FAIL);
            }

            log.info("秒杀抢号成功，scheduleId={}, patientId={}, orderSn={}", scheduleId, patientId, orderSn);
            return new SeckillResponse(orderSn, "QUEUING");

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 实体转 VO
     */
    private ScheduleVO toVO(DoctorSchedule schedule) {
        ScheduleVO vo = new ScheduleVO();
        vo.setId(schedule.getId());
        vo.setDoctorId(schedule.getDoctorId());
        vo.setDeptName(schedule.getDeptName());
        vo.setWorkDate(schedule.getWorkDate());
        vo.setShift(schedule.getShift());
        vo.setShiftName(schedule.getShift() == 1 ? "上午" : "下午");
        vo.setTotalCount(schedule.getTotalCount());
        vo.setVisibleCount(schedule.getVisibleCount());
        vo.setPrice(schedule.getPrice());
        return vo;
    }
}
