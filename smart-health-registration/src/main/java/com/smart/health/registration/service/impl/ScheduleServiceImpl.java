package com.smart.health.registration.service.impl;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.ScheduleVO;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 排班管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final DoctorScheduleMapper doctorScheduleMapper;
    private final StringRedisTemplate stringRedisTemplate;

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
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + schedule.getId();
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(request.getTotalCount()));

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

        // 1. 校验排班是否存在
        DoctorSchedule schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }

        // 2. Redis 预扣库存
        String stockKey = CommonConstants.REDIS_SCHEDULE_STOCK_PREFIX + scheduleId;
        Long remain = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (remain == null || remain < 0) {
            // 回滚 Redis 库存
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException(ResultCode.STOCK_EMPTY);
        }

        // 3. 生成订单号
        String orderSn = CommonConstants.ORDER_SN_PREFIX
                + LocalDate.now().format(DATE_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        log.info("秒杀抢号成功，scheduleId={}, patientId={}, orderSn={}", scheduleId, request.getPatientId(), orderSn);

        // 4. 返回排队中状态（后续接入 MQ 异步出单）
        return new SeckillResponse(orderSn, "QUEUING");
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
