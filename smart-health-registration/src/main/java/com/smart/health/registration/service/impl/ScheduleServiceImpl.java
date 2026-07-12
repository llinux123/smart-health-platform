package com.smart.health.registration.service.impl;

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
import com.smart.health.registration.mapper.DoctorMapper;
import com.smart.health.registration.service.ScheduleService;
import com.smart.health.registration.util.OrderSnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final DoctorMapper doctorMapper;
    private final ScheduleRedisConfig scheduleRedisConfig;
    private final RabbitTemplate rabbitTemplate;
    private final OrderSnGenerator orderSnGenerator;

    private static final int SHIFT_MORNING = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSchedule(ScheduleCreateRequest request) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorId(request.getDoctorId());
        if (request.getDepartmentId() != null) {
            schedule.setDepartmentId(request.getDepartmentId());
        } else {
            // 未传科室ID时，自动从医生主科室补全
            Long primaryDeptId = doctorMapper.selectPrimaryDepartmentId(request.getDoctorId());
            if (primaryDeptId != null) {
                schedule.setDepartmentId(primaryDeptId);
            }
        }
        schedule.setDeptName(request.getDeptName());
        schedule.setWorkDate(request.getWorkDate());
        schedule.setShift(request.getShift());
        schedule.setTotalCount(request.getTotalCount());
        schedule.setVisibleCount(request.getTotalCount());
        schedule.setPrice(request.getPrice());

        doctorScheduleMapper.insert(schedule);

        // 写穿透：库存与价格一起初始化到 Redis，秒杀热路径无需访问 DB
        scheduleRedisConfig.initScheduleStock(schedule.getId(), request.getTotalCount(), request.getPrice());

        log.info("创建排班成功，scheduleId={}, stock={}", schedule.getId(), request.getTotalCount());
    }

    @Override
    public List<ScheduleVO> getAvailableSchedules(String deptName, Long departmentId, LocalDate workDate) {
        List<DoctorSchedule> list = doctorScheduleMapper.selectAvailableList(deptName, departmentId, workDate);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ScheduleVO getScheduleDetail(Long scheduleId) {
        DoctorSchedule schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        return toVO(schedule);
    }

    @Override
    public SeckillResponse seckill(SeckillRequest request, Long patientId) {
        Long scheduleId = request.getScheduleId();

        // 1. Redis 存在性检查（替代热路径上的 DB 查询）：库存 key 不存在视为排班无效
        //    同时预热检查价格缓存（写穿透时已写入，缓存未命中则按 SCHEDULE_NOT_FOUND 处理）
        if (scheduleRedisConfig.getScheduleStock(scheduleId) == null
                || scheduleRedisConfig.getSchedulePrice(scheduleId) == null) {
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
            // 4. 双重检查：持锁后再校验幂等，避免锁窗口内的并发抢号
            if (scheduleRedisConfig.isPatientInSeckillSet(scheduleId, patientId)) {
                throw new BusinessException(ResultCode.REPEAT_SECKILL);
            }

            // 5. Redis 原子预扣库存
            Long remain = scheduleRedisConfig.decrementStock(scheduleId);
            if (remain == null) {
                // 锁内再次确认 key 缺失：缓存异常或排班已下架
                throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
            }
            if (remain < 0) {
                // 超卖保护：超出则回滚本次 DECR 并提示库存为空
                scheduleRedisConfig.incrementStock(scheduleId);
                throw new BusinessException(ResultCode.STOCK_EMPTY);
            }

            // 6. 生成订单号
            String orderSn = orderSnGenerator.generate();

            // 7. 标记患者已抢（幂等性）
            scheduleRedisConfig.addPatientToSeckillSet(scheduleId, patientId);

            // 8. 发送 MQ 消息异步创建订单（使用 Jackson Converter，避免手工 JSON 序列化）
            BigDecimal price = scheduleRedisConfig.getSchedulePrice(scheduleId);
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setOrderSn(orderSn);
            message.setPatientId(patientId);
            message.setScheduleId(scheduleId);
            message.setAmount(price);

            try {
                rabbitTemplate.convertAndSend(
                        CommonConstants.MQ_EXCHANGE_REGISTRATION,
                        CommonConstants.MQ_ROUTING_KEY_ORDER_CREATE,
                        message
                );
                log.info("秒杀订单消息已发送，orderSn={}, scheduleId={}, patientId={}", orderSn, scheduleId, patientId);
            } catch (Exception e) {
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
        vo.setDoctorName(schedule.getDoctorName());
        vo.setDoctorAvatar(schedule.getDoctorAvatar());
        vo.setDepartmentId(schedule.getDepartmentId());
        vo.setDeptName(schedule.getDeptName());
        vo.setWorkDate(schedule.getWorkDate());
        vo.setShift(schedule.getShift());
        vo.setShiftName(schedule.getShift() != null && schedule.getShift() == SHIFT_MORNING ? "上午" : "下午");
        vo.setTotalCount(schedule.getTotalCount());
        vo.setVisibleCount(schedule.getVisibleCount());
        vo.setPrice(schedule.getPrice());
        return vo;
    }
}
