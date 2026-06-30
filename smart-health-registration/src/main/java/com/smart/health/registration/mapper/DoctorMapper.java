package com.smart.health.registration.mapper;

import com.smart.health.registration.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 医生信息 Mapper
 */
@Mapper
public interface DoctorMapper {

    /**
     * 根据ID查询医生信息
     */
    @Select("SELECT id, name, title, avatar, dept_name, specialty, intro, create_time " +
            "FROM t_doctor WHERE id = #{id}")
    Doctor selectById(@Param("id") Long id);
}
