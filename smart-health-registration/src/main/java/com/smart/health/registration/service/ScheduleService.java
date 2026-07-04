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
    List<ScheduleVO> getAvailableSchedules(String deptName, Long departmentId, LocalDate workDate);

    /**
     * 获取排班详情（含医生头像、科室信息）
     */
    ScheduleVO getScheduleDetail(Long scheduleId);

    /**
     * 秒杀抢号
     *
     * @param request   抢号请求（仅含业务参数 scheduleId）
     * @param patientId 当前认证患者ID（由 Controller 从 SecurityContext 获取后传入）
     */
    SeckillResponse seckill(SeckillRequest request, Long patientId);
}
