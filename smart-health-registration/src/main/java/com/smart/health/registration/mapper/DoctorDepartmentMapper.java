package com.smart.health.registration.mapper;

import com.smart.health.registration.entity.DoctorDepartment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 医生-科室关联 Mapper
 */
@Mapper
public interface DoctorDepartmentMapper {

    @Insert("INSERT INTO t_doctor_department (doctor_id, department_id, is_primary) " +
            "VALUES (#{doctorId}, #{departmentId}, #{isPrimary})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DoctorDepartment dd);

    @Select("SELECT id, doctor_id, department_id, is_primary, create_time " +
            "FROM t_doctor_department WHERE doctor_id = #{doctorId}")
    List<DoctorDepartment> selectByDoctorId(@Param("doctorId") Long doctorId);

    @Select("SELECT dd.id, dd.doctor_id, dd.department_id, dd.is_primary, dd.create_time " +
            "FROM t_doctor_department dd WHERE dd.department_id = #{departmentId}")
    List<DoctorDepartment> selectDoctorsByDeptId(@Param("departmentId") Long departmentId);

    @Delete("DELETE FROM t_doctor_department WHERE doctor_id = #{doctorId} AND department_id = #{departmentId}")
    int delete(@Param("doctorId") Long doctorId, @Param("departmentId") Long departmentId);
}
