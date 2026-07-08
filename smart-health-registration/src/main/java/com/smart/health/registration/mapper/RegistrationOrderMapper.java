package com.smart.health.registration.mapper;

import com.smart.health.registration.dto.OrderVO;
import com.smart.health.registration.entity.RegistrationOrder;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 挂号订单Mapper
 */
@Mapper
public interface RegistrationOrderMapper {

    @Insert("INSERT INTO t_registration_order (order_sn, patient_id, schedule_id, sequence_number, amount, status) " +
            "VALUES (#{orderSn}, #{patientId}, #{scheduleId}, #{sequenceNumber}, #{amount}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RegistrationOrder order);

    @Select("SELECT id, order_sn, patient_id, schedule_id, sequence_number, amount, status, pay_time, create_time " +
            "FROM t_registration_order WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<RegistrationOrder> selectByPatientId(@Param("patientId") Long patientId);

    @Select("SELECT id, order_sn, patient_id, schedule_id, sequence_number, amount, status, pay_time, create_time " +
            "FROM t_registration_order WHERE order_sn = #{orderSn}")
    RegistrationOrder selectByOrderSn(@Param("orderSn") String orderSn);

    @Select("SELECT COALESCE(MAX(sequence_number), 0) FROM t_registration_order WHERE schedule_id = #{scheduleId}")
    int selectMaxSequenceNumber(@Param("scheduleId") Long scheduleId);

    /**
     * 查询订单列表（JOIN 排班及医生表，返回 OrderVO）
     */
    @Select("SELECT o.order_sn, o.patient_id, o.schedule_id, " +
            "s.dept_name, d.name AS doctor_name, s.work_date, s.shift, " +
            "CASE s.shift WHEN 1 THEN '上午' WHEN 2 THEN '下午' ELSE '未知' END AS shift_name, " +
            "s.price AS fee, o.status, o.create_time, o.pay_time " +
            "FROM t_registration_order o " +
            "LEFT JOIN t_doctor_schedule s ON o.schedule_id = s.id " +
            "LEFT JOIN t_doctor d ON s.doctor_id = d.id " +
            "WHERE o.patient_id = #{patientId} " +
            "ORDER BY o.create_time DESC")
    List<OrderVO> selectOrderVOByPatientId(@Param("patientId") Long patientId);

    /**
     * 根据订单号查询订单（JOIN 排班及医生表，返回 OrderVO）
     */
    @Select("SELECT o.order_sn, o.patient_id, o.schedule_id, " +
            "s.dept_name, d.name AS doctor_name, s.work_date, s.shift, " +
            "CASE s.shift WHEN 1 THEN '上午' WHEN 2 THEN '下午' ELSE '未知' END AS shift_name, " +
            "s.price AS fee, o.status, o.create_time, o.pay_time " +
            "FROM t_registration_order o " +
            "LEFT JOIN t_doctor_schedule s ON o.schedule_id = s.id " +
            "LEFT JOIN t_doctor d ON s.doctor_id = d.id " +
            "WHERE o.order_sn = #{orderSn}")
    OrderVO selectOrderVOByOrderSn(@Param("orderSn") String orderSn);

    /**
     * 按订单号 + 患者ID查询订单视图，单次 SQL 完成归属校验与 VO 装配
     * 返回 null 表示订单不存在或归属不符
     */
    @Select("SELECT o.order_sn, o.patient_id, o.schedule_id, " +
            "s.dept_name, d.name AS doctor_name, s.work_date, s.shift, " +
            "CASE s.shift WHEN 1 THEN '上午' WHEN 2 THEN '下午' ELSE '未知' END AS shift_name, " +
            "s.price AS fee, o.status, o.create_time, o.pay_time " +
            "FROM t_registration_order o " +
            "LEFT JOIN t_doctor_schedule s ON o.schedule_id = s.id " +
            "LEFT JOIN t_doctor d ON s.doctor_id = d.id " +
            "WHERE o.order_sn = #{orderSn} AND o.patient_id = #{patientId}")
    OrderVO selectOrderVOByOrderSnAndPatientId(@Param("orderSn") String orderSn,
                                               @Param("patientId") Long patientId);

    /**
     * 分页查询患者订单列表（按创建时间倒序）
     *
     * @param patientId 患者ID
     * @param offset    偏移量
     * @param limit     每页大小
     */
    @Select("SELECT o.order_sn, o.patient_id, o.schedule_id, " +
            "s.dept_name, d.name AS doctor_name, s.work_date, s.shift, " +
            "CASE s.shift WHEN 1 THEN '上午' WHEN 2 THEN '下午' ELSE '未知' END AS shift_name, " +
            "s.price AS fee, o.status, o.create_time, o.pay_time " +
            "FROM t_registration_order o " +
            "LEFT JOIN t_doctor_schedule s ON o.schedule_id = s.id " +
            "LEFT JOIN t_doctor d ON s.doctor_id = d.id " +
            "WHERE o.patient_id = #{patientId} " +
            "ORDER BY o.create_time DESC, o.id DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<OrderVO> selectOrderVOByPatientIdPaged(@Param("patientId") Long patientId,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    /**
     * 更新订单状态（仅允许从指定源状态流转，保证幂等性）
     *
     * @param orderSn     订单号
     * @param targetStatus 目标状态
     * @param fromStatuses 允许的源状态列表
     * @return 受影响行数
     */
    @Update("<script>" +
            "UPDATE t_registration_order SET status = #{targetStatus} " +
            "WHERE order_sn = #{orderSn} AND status IN " +
            "<foreach collection='fromStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</script>")
    int updateStatus(@Param("orderSn") String orderSn,
                     @Param("targetStatus") int targetStatus,
                     @Param("fromStatuses") List<Integer> fromStatuses);

    /**
     * 更新订单状态并记录支付时间
     */
    @Update("<script>" +
            "UPDATE t_registration_order SET status = #{targetStatus}, pay_time = #{payTime} " +
            "WHERE order_sn = #{orderSn} AND status IN " +
            "<foreach collection='fromStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach>" +
            "</script>")
    int updateStatusWithPayTime(@Param("orderSn") String orderSn,
                                @Param("targetStatus") int targetStatus,
                                @Param("fromStatuses") List<Integer> fromStatuses,
                                @Param("payTime") LocalDateTime payTime);

    @Select("SELECT COUNT(*) FROM t_registration_order WHERE patient_id = #{patientId}")
    int countByPatientId(@Param("patientId") Long patientId);
}
