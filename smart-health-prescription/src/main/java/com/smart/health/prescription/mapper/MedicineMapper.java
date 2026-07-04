package com.smart.health.prescription.mapper;

import com.smart.health.prescription.entity.Medicine;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 药品字典 Mapper
 */
@Mapper
public interface MedicineMapper {

    /**
     * 按关键字搜索药品（前缀优先 + 模糊匹配）
     */
    @Select("<script>" +
            "SELECT id, name, brand_name, category, spec, unit, manufacturer, price " +
            "FROM t_medicine WHERE is_active = 1 " +
            "AND (name LIKE CONCAT(#{keyword}, '%') " +
            "  OR name LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR brand_name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY CASE WHEN name LIKE CONCAT(#{keyword}, '%') THEN 0 ELSE 1 END, name ASC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Medicine> selectByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT id, name, brand_name, category, spec, unit, manufacturer, approval_number, price, is_otc, is_active, create_time " +
            "FROM t_medicine WHERE id = #{id}")
    Medicine selectById(@Param("id") Long id);

    @Select("SELECT id, name, brand_name, category, spec, unit, manufacturer, price " +
            "FROM t_medicine WHERE is_active = 1 ORDER BY name ASC LIMIT #{offset}, #{size}")
    List<Medicine> selectList(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM t_medicine WHERE is_active = 1")
    int countActive();
}
