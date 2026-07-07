package com.smart.health.consultation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 患者信息查询 Mapper（在 consultation 模块中直接查询 t_patient）
 */
@Mapper
public interface PatientMapper {

    @Select("SELECT real_name FROM t_patient WHERE id = #{id}")
    String selectNameById(@Param("id") Long id);

    @Select("SELECT gender FROM t_patient WHERE id = #{id}")
    Integer selectGenderById(@Param("id") Long id);

    @Select("SELECT birthday FROM t_patient WHERE id = #{id}")
    java.sql.Date selectBirthdayById(@Param("id") Long id);
}
