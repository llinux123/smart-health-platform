package com.smart.health.registration.service.impl;

import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.mapper.RegistrationOrderMapper;
import com.smart.health.registration.service.RegistrationOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 挂号订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationOrderServiceImpl implements RegistrationOrderService {

    private final RegistrationOrderMapper registrationOrderMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationOrder createOrder(RegistrationOrder order) {
        // 查询当前排班最大呼叫序号
        int maxSequence = registrationOrderMapper.selectMaxSequenceNumber(order.getScheduleId());
        order.setSequenceNumber(maxSequence + 1);
        order.setStatus(0); // 排队中

        registrationOrderMapper.insert(order);
        log.info("创建挂号订单成功，orderSn={}, scheduleId={}, sequenceNumber={}",
                order.getOrderSn(), order.getScheduleId(), order.getSequenceNumber());

        // 同步 DB 库存：乐观锁扣减 visible_count
        syncDbInventory(order.getScheduleId());

        return order;
    }

    @Override
    public RegistrationOrder getByOrderSn(String orderSn) {
        return registrationOrderMapper.selectByOrderSn(orderSn);
    }

    @Override
    public List<RegistrationOrder> listByPatientId(Long patientId) {
        return registrationOrderMapper.selectByPatientId(patientId);
    }

    /**
     * 同步 DB 库存：乐观锁扣减 visible_count
     * Redis 已保证不超卖，DB 为最终一致性；乐观锁冲突时仅记录告警，不抛异常
     */
    private void syncDbInventory(Long scheduleId) {
        DoctorSchedule schedule = doctorScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("同步DB库存失败：排班不存在，scheduleId={}", scheduleId);
            return;
        }
        int rows = doctorScheduleMapper.decrementVisibleCount(scheduleId, schedule.getVersion());
        if (rows == 0) {
            log.warn("同步DB库存乐观锁冲突，scheduleId={}, version={}", scheduleId, schedule.getVersion());
        } else {
            log.info("同步DB库存成功，scheduleId={}", scheduleId);
        }
    }
}
