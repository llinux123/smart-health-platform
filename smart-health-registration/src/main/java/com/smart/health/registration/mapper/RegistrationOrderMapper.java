package com.smart.health.registration.mapper;

import com.smart.health.registration.entity.RegistrationOrder;
import org.apache.ibatis.annotations.*;

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
}
