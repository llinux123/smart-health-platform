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
    @Insert("INSERT INTO t_doctor_schedule (doctor_id, department_id, dept_name, work_date, shift, total_count, visible_count, price, version) " +
            "VALUES (#{doctorId}, #{departmentId}, #{deptName}, #{workDate}, #{shift}, #{totalCount}, #{visibleCount}, #{price}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DoctorSchedule schedule);

    /**
     * 查询可用排班列表（剩余号源 > 0），JOIN医生表获取医生姓名和头像
     */
    @Select("<script>" +
            "SELECT s.id, s.doctor_id, d.name AS doctor_name, d.avatar AS doctor_avatar, " +
            "s.department_id, s.dept_name, s.work_date, s.shift, s.total_count, s.visible_count, s.price, s.version " +
            "FROM t_doctor_schedule s LEFT JOIN t_doctor d ON s.doctor_id = d.id " +
            "WHERE s.visible_count &gt; 0" +
            "<if test='deptName != null and deptName != \"\"'> AND s.dept_name = #{deptName}</if>" +
            "<if test='departmentId != null'> AND s.department_id = #{departmentId}</if>" +
            "<if test='workDate != null'> AND s.work_date = #{workDate}</if>" +
            " ORDER BY s.work_date ASC, s.shift ASC" +
            "</script>")
    List<DoctorSchedule> selectAvailableList(@Param("deptName") String deptName,
                                              @Param("departmentId") Long departmentId,
                                              @Param("workDate") LocalDate workDate);

    /**
     * 根据ID查询排班（含医生详情）
     */
    @Select("SELECT s.id, s.doctor_id, d.name AS doctor_name, d.avatar AS doctor_avatar, " +
            "s.department_id, s.dept_name, s.work_date, s.shift, s.total_count, s.visible_count, s.price, s.version " +
            "FROM t_doctor_schedule s LEFT JOIN t_doctor d ON s.doctor_id = d.id WHERE s.id = #{id}")
    DoctorSchedule selectById(@Param("id") Long id);

    /**
     * 乐观锁扣减号源
     */
    @Update("UPDATE t_doctor_schedule SET visible_count = visible_count - 1, version = version + 1 " +
            "WHERE id = #{id} AND version = #{version} AND visible_count > 0")
    int decrementVisibleCount(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 乐观锁恢复号源（取消订单时调用）
     */
    @Update("UPDATE t_doctor_schedule SET visible_count = visible_count + 1, version = version + 1 " +
            "WHERE id = #{id} AND version = #{version} AND visible_count < total_count")
    int incrementVisibleCount(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 无条件恢复号源（取消订单时调用）。
     * 仅依赖 {@code visible_count < total_count} 上限保护，
     * 适用于：版本号已被并发抢占、但取消本身是低频写操作的场景。
     * 比乐观锁路径少一次 SELECT。
     */
    @Update("UPDATE t_doctor_schedule SET visible_count = visible_count + 1, version = version + 1 " +
            "WHERE id = #{id} AND visible_count < total_count")
    int incrementVisibleCountUnchecked(@Param("id") Long id);
}
