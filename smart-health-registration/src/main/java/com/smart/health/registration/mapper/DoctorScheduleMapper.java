package com.smart.health.registration.mapper;

import com.smart.health.registration.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 医生排班号源 Mapper
 */
@Mapper
public interface DoctorScheduleMapper {

    /**
     * 新增排班
     */
    @Insert("INSERT INTO t_doctor_schedule (doctor_id, dept_name, work_date, shift, total_count, visible_count, price, version) " +
            "VALUES (#{doctorId}, #{deptName}, #{workDate}, #{shift}, #{totalCount}, #{visibleCount}, #{price}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DoctorSchedule schedule);

    /**
     * 查询可用排班列表（剩余号源 > 0）
     */
    @Select("<script>" +
            "SELECT id, doctor_id, dept_name, work_date, shift, total_count, visible_count, price, version " +
            "FROM t_doctor_schedule WHERE visible_count &gt; 0" +
            "<if test='deptName != null and deptName != \"\"'> AND dept_name = #{deptName}</if>" +
            "<if test='workDate != null'> AND work_date = #{workDate}</if>" +
            " ORDER BY work_date ASC, shift ASC" +
            "</script>")
    List<DoctorSchedule> selectAvailableList(@Param("deptName") String deptName, @Param("workDate") LocalDate workDate);

    /**
     * 根据ID查询排班
     */
    @Select("SELECT id, doctor_id, dept_name, work_date, shift, total_count, visible_count, price, version " +
            "FROM t_doctor_schedule WHERE id = #{id}")
    DoctorSchedule selectById(@Param("id") Long id);

    /**
     * 乐观锁扣减号源
     */
    @Update("UPDATE t_doctor_schedule SET visible_count = visible_count - 1, version = version + 1 " +
            "WHERE id = #{id} AND version = #{version} AND visible_count &gt; 0")
    int decrementVisibleCount(@Param("id") Long id, @Param("version") Integer version);
}
