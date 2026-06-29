package com.smart.health.registration.service;

import com.smart.health.registration.dto.ScheduleCreateRequest;
import com.smart.health.registration.dto.ScheduleVO;
import com.smart.health.registration.dto.SeckillRequest;
import com.smart.health.registration.dto.SeckillResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班管理服务接口
 */
public interface ScheduleService {

    /**
     * 创建排班
     */
    void createSchedule(ScheduleCreateRequest request);

    /**
     * 查询可预约排班列表
     */
    List<ScheduleVO> getAvailableSchedules(String deptName, LocalDate workDate);

    /**
     * 秒杀抢号
     */
    SeckillResponse seckill(SeckillRequest request);
}
