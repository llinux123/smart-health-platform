package com.smart.health.registration.service;

import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.entity.RegistrationOrder;

import java.util.List;

/**
 * 挂号订单服务接口
 */
public interface RegistrationOrderService {

    /**
     * 创建挂号订单
     *
     * @param order 订单信息
     * @return 创建的订单
     */
    RegistrationOrder createOrder(RegistrationOrder order);

    /**
     * 根据订单号查询订单
     *
     * @param orderSn 订单号
     * @return 订单信息
     */
    RegistrationOrder getByOrderSn(String orderSn);

    /**
     * 查询患者的挂号订单列表
     *
     * @param patientId 患者ID
     * @return 订单列表
     */
    List<RegistrationOrder> listByPatientId(Long patientId);

    /**
     * 根据订单号查询订单视图（包含排班及医生信息）
     *
     * @param orderSn 订单号
     * @return 订单视图
     */
    OrderVO getOrderVOByOrderSn(String orderSn);

    /**
     * 查询患者的挂号订单列表视图（包含排班及医生信息）
     *
     * @param patientId 患者ID
     * @return 订单视图列表
     */
    List<OrderVO> listOrderVOByPatientId(Long patientId);

    /**
     * 取消订单（状态变更为 4-已退号）
     *
     * @param orderSn 订单号
     */
    void cancelOrder(String orderSn);

    /**
     * 支付订单（状态变更为 2-已支付，记录支付时间）
     *
     * @param orderSn 订单号
     */
    void payOrder(String orderSn);
}
