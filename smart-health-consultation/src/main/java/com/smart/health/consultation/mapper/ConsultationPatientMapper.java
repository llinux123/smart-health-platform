package com.smart.health.consultation.mapper;

import com.smart.health.consultation.dto.PatientInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 患者信息查询 Mapper（在 consultation 模块中直接查询 t_patient）
 */
@Mapper
public interface ConsultationPatientMapper {

    @Select("SELECT real_name FROM t_patient WHERE id = #{id}")
    String selectNameById(@Param("id") Long id);

    @Select("SELECT gender FROM t_patient WHERE id = #{id}")
    Integer selectGenderById(@Param("id") Long id);

    @Select("SELECT birthday FROM t_patient WHERE id = #{id}")
    java.sql.Date selectBirthdayById(@Param("id") Long id);

    @Select({"<script>",
             "SELECT id, real_name AS name, gender, birthday FROM t_patient WHERE id IN",
             "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
             "</script>"})
    List<PatientInfo> selectBatchInfo(@Param("ids") List<Long> ids);
}
