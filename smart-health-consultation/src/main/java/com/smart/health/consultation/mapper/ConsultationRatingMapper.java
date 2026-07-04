package com.smart.health.consultation.mapper;

import com.smart.health.consultation.entity.ConsultationRating;
import org.apache.ibatis.annotations.*;

/**
 * 问诊评分Mapper
 */
@Mapper
public interface ConsultationRatingMapper {

    /**
     * 插入评分
     */
    @Insert("INSERT INTO t_consultation_rating (session_sn, patient_id, rating, feedback) " +
            "VALUES (#{sessionSn}, #{patientId}, #{rating}, #{feedback})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationRating rating);

    /**
     * 查询会话是否已评分
     */
    @Select("SELECT COUNT(*) > 0 FROM t_consultation_rating WHERE session_sn = #{sessionSn}")
    boolean existsBySessionSn(@Param("sessionSn") String sessionSn);

    /**
     * 根据会话编号查询评分
     */
    @Select("SELECT * FROM t_consultation_rating WHERE session_sn = #{sessionSn}")
    ConsultationRating selectBySessionSn(@Param("sessionSn") String sessionSn);
}
