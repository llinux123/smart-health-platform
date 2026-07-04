package com.smart.health.user.mapper;

import com.smart.health.user.entity.Staff;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 员工Mapper
 */
@Mapper
public interface StaffMapper {

    @Insert("INSERT INTO t_staff (username, password, real_name, phone, role, doctor_id) " +
            "VALUES (#{username}, #{password}, #{realName}, #{phone}, #{role}, #{doctorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Staff staff);

    @Select("SELECT * FROM t_staff WHERE username = #{username} AND is_deleted = 0")
    Staff findByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_staff WHERE id = #{id} AND is_deleted = 0")
    Staff findById(@Param("id") Long id);

    @Select("SELECT * FROM t_staff WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<Staff> findAll();

    @Select("SELECT * FROM t_staff WHERE role = #{role} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Staff> findByRole(@Param("role") String role);

    @Update({"<script>",
            "UPDATE t_staff SET",
            "  real_name = #{realName},",
            "  phone = #{phone},",
            "  <if test='password != null'>password = #{password},</if>",
            "  <if test='doctorId != null'>doctor_id = #{doctorId},</if>",
            "  update_time = NOW()",
            "WHERE id = #{id} AND is_deleted = 0",
            "</script>"})
    int update(Staff staff);

    @Update("UPDATE t_staff SET is_deleted = 1, deleted_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM t_staff WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(@Param("username") String username);
}
