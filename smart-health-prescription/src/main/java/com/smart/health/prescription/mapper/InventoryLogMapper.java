package com.smart.health.prescription.mapper;

import com.smart.health.prescription.entity.InventoryLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 库存变动日志 Mapper
 */
@Mapper
public interface InventoryLogMapper {

    @Insert("INSERT INTO t_inventory_log (pharmacy_id, medicine_id, change_type, quantity_change, stock_before, stock_after, reason, operator_id) " +
            "VALUES (#{pharmacyId}, #{medicineId}, #{changeType}, #{quantityChange}, #{stockBefore}, #{stockAfter}, #{reason}, #{operatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InventoryLog log);

    @Select("<script>" +
            "SELECT id, pharmacy_id, medicine_id, change_type, quantity_change, stock_before, stock_after, reason, operator_id, create_time " +
            "FROM t_inventory_log WHERE 1=1 " +
            "<if test='pharmacyId != null'> AND pharmacy_id = #{pharmacyId}</if>" +
            "<if test='medicineId != null'> AND medicine_id = #{medicineId}</if>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{size}" +
            "</script>")
    List<InventoryLog> selectByPage(@Param("pharmacyId") Long pharmacyId,
                                     @Param("medicineId") Long medicineId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_inventory_log WHERE 1=1 " +
            "<if test='pharmacyId != null'> AND pharmacy_id = #{pharmacyId}</if>" +
            "<if test='medicineId != null'> AND medicine_id = #{medicineId}</if>" +
            "</script>")
    int countByCondition(@Param("pharmacyId") Long pharmacyId,
                         @Param("medicineId") Long medicineId);
}
