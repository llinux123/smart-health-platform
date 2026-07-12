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
     * 根据ID查询医生信息（LEFT JOIN 关联表获取主科室ID）
     */
    @Select("SELECT d.id, d.name, d.title, d.avatar, d.dept_name, d.specialty, d.intro, d.create_time, " +
            "dd.department_id AS primary_department_id " +
            "FROM t_doctor d " +
            "LEFT JOIN t_doctor_department dd ON d.id = dd.doctor_id AND dd.is_primary = 1 " +
            "WHERE d.id = #{id}")
    Doctor selectById(@Param("id") Long id);
    /**
     * 查询医生的主科室ID
     */
    @Select("SELECT department_id FROM t_doctor_department WHERE doctor_id = #{doctorId} AND is_primary = 1 LIMIT 1")
    Long selectPrimaryDepartmentId(@Param("doctorId") Long doctorId);
}
