package com.smart.health.user.mapper;

import com.smart.health.user.entity.Patient;
import org.apache.ibatis.annotations.*;

/**
 * 患者用户Mapper
 */
@Mapper
public interface PatientMapper {

    @Insert("INSERT INTO t_patient (username, password, real_name, id_card, phone, gender) " +
            "VALUES (#{username}, #{password}, #{realName}, #{idCard}, #{phone}, #{gender})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Patient patient);

    @Select("SELECT * FROM t_patient WHERE username = #{username}")
    Patient findByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_patient WHERE id = #{id}")
    Patient findById(@Param("id") Long id);

    @Select("SELECT * FROM t_patient WHERE phone = #{phone}")
    Patient findByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM t_patient WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM t_patient WHERE id_card = #{idCard}")
    int countByIdCard(@Param("idCard") String idCard);

    @Select("SELECT COUNT(*) FROM t_patient WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
}
