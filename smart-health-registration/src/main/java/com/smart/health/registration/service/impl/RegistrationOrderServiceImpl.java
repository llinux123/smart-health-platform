package com.smart.health.registration.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.PageResult;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.config.ScheduleRedisConfig;
import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.entity.DoctorSchedule;
import com.smart.health.registration.entity.RegistrationOrder;
import com.smart.health.registration.mapper.DoctorScheduleMapper;
import com.smart.health.registration.mapper.RegistrationOrderMapper;
import com.smart.health.registration.service.RegistrationOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 挂号订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationOrderServiceImpl implements RegistrationOrderService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RegistrationOrderMapper registrationOrderMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final ScheduleRedisConfig scheduleRedisConfig;

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

    @Override
    public OrderVO getOrderVOByOrderSn(String orderSn, Long patientId, String role) {
        OrderVO vo;
        if ("PATIENT".equals(role)) {
            // PATIENT 角色：单条 SQL 完成归属校验与 VO 装配（替代原先 selectByOrderSn + selectOrderVOByOrderSn 两次查询）
            vo = registrationOrderMapper.selectOrderVOByOrderSnAndPatientId(orderSn, patientId);
            if (vo == null) {
                throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或无权查看");
            }
            return vo;
        }
        // ADMIN / DOCTOR：跳过归属校验，单条 JOIN 查询
        vo = registrationOrderMapper.selectOrderVOByOrderSn(orderSn);
        if (vo == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return vo;
    }

    @Override
    public List<OrderVO> listOrderVOByPatientId(Long patientId) {
        return registrationOrderMapper.selectOrderVOByPatientId(patientId);
    }

    @Override
    public PageResult<OrderVO> pageOrderVOByPatientId(Long patientId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        long total = registrationOrderMapper.countByPatientId(patientId);
        List<OrderVO> list = registrationOrderMapper.selectOrderVOByPatientIdPaged(patientId, offset, safeSize);
        return PageResult.of(list, total, safePage, safeSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderSn, Long patientId) {
        RegistrationOrder order = findAndValidateOrder(orderSn, patientId);
        // 幂等处理：已是已退号状态直接返回
        if (order.getStatus() == 4) {
            log.info("订单已是已退号状态，忽略重复取消，orderSn={}", orderSn);
            return;
        }
        // 仅排队中(0)、待支付(1)可取消
        int rows = registrationOrderMapper.updateStatus(orderSn, 4, List.of(0, 1));
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
        }

        // 恢复号源库存
        restoreInventory(order.getScheduleId(), order.getPatientId());
        log.info("取消订单成功，orderSn={}, scheduleId={}, patientId={}", orderSn, order.getScheduleId(), order.getPatientId());
    }

    /**
     * 恢复号源库存：Redis 库存回滚 + DB visibleCount 上限恢复 + 移除幂等集合
     *
     * <p>取消是低频写操作，DB 恢复使用 {@code visible_count < total_count} 的上限保护，
     * 不再走乐观锁（避免版本号过期造成的"恢复失败"误报）。
     */
    private void restoreInventory(Long scheduleId, Long patientId) {
        // 1. 恢复 Redis 库存
        scheduleRedisConfig.incrementStock(scheduleId);
        log.info("取消订单恢复Redis库存，scheduleId={}", scheduleId);

        // 2. 恢复 DB visibleCount（仅依赖上限保护，少一次 SELECT）
        int rows = doctorScheduleMapper.incrementVisibleCountUnchecked(scheduleId);
        if (rows == 0) {
            log.warn("恢复DB号源失败：可能已满或排班不存在，scheduleId={}", scheduleId);
        } else {
            log.info("恢复DB号源成功，scheduleId={}", scheduleId);
        }

        // 3. 从已抢集合中移除患者，允许再次预约同一专家
        scheduleRedisConfig.removePatientFromSeckillSet(scheduleId, patientId);
        log.info("移除患者幂等集合，scheduleId={}, patientId={}", scheduleId, patientId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderSn, Long patientId) {
        RegistrationOrder order = findAndValidateOrder(orderSn, patientId);
        // 幂等处理：已是已支付状态直接返回
        if (order.getStatus() == 2) {
            log.info("订单已是已支付状态，忽略重复支付，orderSn={}", orderSn);
            return;
        }
        // 仅排队中(0)、待支付(1)可支付
        int rows = registrationOrderMapper.updateStatusWithPayTime(orderSn, 2, List.of(0, 1), LocalDateTime.now());
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不允许支付");
        }
        log.info("支付订单成功，orderSn={}", orderSn);
    }

    /**
     * 查找订单并校验归属（防止越权操作）
     *
     * @param orderSn   订单号
     * @param patientId 当前认证患者ID
     * @return 已验证归属的订单实体
     * @throws BusinessException 订单不存在或归属不符时抛出
     */
    private RegistrationOrder findAndValidateOrder(String orderSn, Long patientId) {
        RegistrationOrder order = registrationOrderMapper.selectByOrderSn(orderSn);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!order.getPatientId().equals(patientId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无权操作该订单");
        }
        return order;
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

    @Override
    public int countByPatientId(Long patientId) {
        return registrationOrderMapper.countByPatientId(patientId);
    }
}
