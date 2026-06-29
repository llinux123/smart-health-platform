package com.smart.health.prescription.mapper;

import com.smart.health.prescription.entity.PrescriptionItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 处方明细Mapper
 */
@Mapper
public interface PrescriptionItemMapper {

    @Insert("INSERT INTO t_prescription_item (prescription_id, medicine_id, medicine_name, pharmacy_id, quantity, unit) " +
            "VALUES (#{prescriptionId}, #{medicineId}, #{medicineName}, #{pharmacyId}, #{quantity}, #{unit})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PrescriptionItem item);

    @Insert("<script>" +
            "INSERT INTO t_prescription_item (prescription_id, medicine_id, medicine_name, pharmacy_id, quantity, unit) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.prescriptionId}, #{item.medicineId}, #{item.medicineName}, #{item.pharmacyId}, #{item.quantity}, #{item.unit})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<PrescriptionItem> items);

    @Select("SELECT id, prescription_id, medicine_id, medicine_name, pharmacy_id, quantity, unit " +
            "FROM t_prescription_item WHERE prescription_id = #{prescriptionId}")
    List<PrescriptionItem> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
