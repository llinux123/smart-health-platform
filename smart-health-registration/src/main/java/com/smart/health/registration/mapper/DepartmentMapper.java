package com.smart.health.registration.mapper;

import com.smart.health.registration.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 科室信息 Mapper
 */
@Mapper
public interface DepartmentMapper {

    @Select("SELECT id, name, description, icon, intro, sort_order, is_active, create_time " +
            "FROM t_department WHERE is_active = 1 ORDER BY sort_order ASC")
    List<Department> selectActive();

    @Select("SELECT id, name, description, icon, intro, sort_order, is_active, create_time " +
            "FROM t_department WHERE id = #{id}")
    Department selectById(@Param("id") Long id);

    @Select("SELECT id, name, description, icon, intro, sort_order, is_active, create_time " +
            "FROM t_department ORDER BY sort_order ASC")
    List<Department> selectAll();
}
