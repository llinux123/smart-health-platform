package com.smart.health.prescription.mapper;

import com.smart.health.prescription.entity.PharmacyInventory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 药房库存Mapper
 */
@Mapper
public interface PharmacyInventoryMapper {

    @Select("SELECT id, pharmacy_id, medicine_id, medicine_name, stock, lock_stock, unit, update_time " +
            "FROM t_pharmacy_inventory WHERE id = #{id}")
    PharmacyInventory selectById(@Param("id") Long id);

    @Select("SELECT id, pharmacy_id, medicine_id, medicine_name, stock, lock_stock, unit, update_time " +
            "FROM t_pharmacy_inventory WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId}")
    PharmacyInventory selectByPharmacyAndMedicine(@Param("pharmacyId") Long pharmacyId,
                                                   @Param("medicineId") Long medicineId);

    @Select("SELECT id, pharmacy_id, medicine_id, medicine_name, stock, lock_stock, unit, update_time " +
            "FROM t_pharmacy_inventory WHERE pharmacy_id = #{pharmacyId}")
    List<PharmacyInventory> selectByPharmacyId(@Param("pharmacyId") Long pharmacyId);

    /**
     * 扣减库存（条件更新：库存充足时才扣减，防止超卖）
     * 返回受影响行数：1=成功，0=库存不足
     */
    @Update("UPDATE t_pharmacy_inventory SET stock = stock - #{quantity} " +
            "WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId} AND stock >= #{quantity}")
    int deductStock(@Param("pharmacyId") Long pharmacyId,
                    @Param("medicineId") Long medicineId,
                    @Param("quantity") int quantity);

    /**
     * 冻结库存（将 stock 转移到 lock_stock）
     */
    @Update("UPDATE t_pharmacy_inventory SET stock = stock - #{quantity}, lock_stock = lock_stock + #{quantity} " +
            "WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId} AND stock >= #{quantity}")
    int lockStock(@Param("pharmacyId") Long pharmacyId,
                  @Param("medicineId") Long medicineId,
                  @Param("quantity") int quantity);

    /**
     * 恢复库存（审核驳回时回补已扣减的库存）
     */
    @Update("UPDATE t_pharmacy_inventory SET stock = stock + #{quantity} " +
            "WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId}")
    int restoreStock(@Param("pharmacyId") Long pharmacyId,
                     @Param("medicineId") Long medicineId,
                     @Param("quantity") int quantity);

    /**
     * 入库（增加库存）
     */
    @Update("UPDATE t_pharmacy_inventory SET stock = stock + #{quantity} " +
            "WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId}")
    int increaseStock(@Param("pharmacyId") Long pharmacyId,
                      @Param("medicineId") Long medicineId,
                      @Param("quantity") int quantity);

    /**
     * 盘点（设置实际库存）
     */
    @Update("UPDATE t_pharmacy_inventory SET stock = #{newStock} " +
            "WHERE pharmacy_id = #{pharmacyId} AND medicine_id = #{medicineId}")
    int setStock(@Param("pharmacyId") Long pharmacyId,
                 @Param("medicineId") Long medicineId,
                 @Param("newStock") int newStock);
}
