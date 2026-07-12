package com.smart.health.consultation.mapper;

import com.smart.health.consultation.entity.ConsultationTurn;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 问诊对话轮次Mapper
 */
@Mapper
public interface ConsultationTurnMapper {

    /**
     * 插入对话轮次
     */
    @Insert("INSERT INTO t_consultation_turn (session_sn, turn_number, user_message, assistant_message, citations, sender_type) " +
            "VALUES (#{sessionSn}, #{turnNumber}, #{userMessage}, #{assistantMessage}, #{citations}, #{senderType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationTurn turn);

    /**
     * 分页查询会话的对话轮次（按 turn_number DESC）
     */
    @Select("SELECT * FROM t_consultation_turn WHERE session_sn = #{sessionSn} ORDER BY turn_number DESC")
    List<ConsultationTurn> selectBySessionSnDesc(@Param("sessionSn") String sessionSn);

    /**
     * 查询会话的对话轮次（按 turn_number ASC）
     */
    @Select("SELECT * FROM t_consultation_turn WHERE session_sn = #{sessionSn} ORDER BY turn_number ASC")
    List<ConsultationTurn> selectBySessionSnAsc(@Param("sessionSn") String sessionSn);

    /**
     * 查询会话的最大轮次号
     */
    @Select("SELECT MAX(turn_number) FROM t_consultation_turn WHERE session_sn = #{sessionSn}")
    Integer selectMaxTurnNumber(@Param("sessionSn") String sessionSn);

    /**
     * 查询会话的总轮次数
     */
    @Select("SELECT COUNT(*) FROM t_consultation_turn WHERE session_sn = #{sessionSn}")
    int countBySessionSn(@Param("sessionSn") String sessionSn);

    /**
     * 根据轮次号查询
     */
    @Select("SELECT * FROM t_consultation_turn WHERE session_sn = #{sessionSn} AND turn_number = #{turnNumber}")
    ConsultationTurn selectByTurnNumber(@Param("sessionSn") String sessionSn, @Param("turnNumber") Integer turnNumber);

    /**
     * 更新AI回复（重新生成时覆盖）
     */
    @Update("UPDATE t_consultation_turn SET assistant_message = #{assistantMessage}, citations = #{citations}, update_time = NOW() " +
            "WHERE session_sn = #{sessionSn} AND turn_number = #{turnNumber}")
    int updateAssistantMessage(@Param("sessionSn") String sessionSn,
                               @Param("turnNumber") Integer turnNumber,
                               @Param("assistantMessage") String assistantMessage,
                               @Param("citations") String citations);

    /**
     * 查询会话的最后一轮（turn_number DESC LIMIT 1）
     */
    @Select("SELECT * FROM t_consultation_turn WHERE session_sn = #{sessionSn} ORDER BY turn_number DESC LIMIT 1")
    ConsultationTurn selectLastTurn(@Param("sessionSn") String sessionSn);

    /**
     * 更新用户消息和AI回复（合并连续消息时使用）
     */
    @Update("UPDATE t_consultation_turn SET user_message = #{userMessage}, assistant_message = #{assistantMessage}, citations = #{citations}, update_time = NOW() " +
            "WHERE session_sn = #{sessionSn} AND turn_number = #{turnNumber}")
    int updateUserAndAssistantMessage(@Param("sessionSn") String sessionSn,
                                      @Param("turnNumber") Integer turnNumber,
                                      @Param("userMessage") String userMessage,
                                      @Param("assistantMessage") String assistantMessage,
                                      @Param("citations") String citations);

    /**
     * 删除会话的所有轮次（物理删除会话时使用）
     */
    @Delete("DELETE FROM t_consultation_turn WHERE session_sn = #{sessionSn}")
    int deleteBySessionSn(@Param("sessionSn") String sessionSn);
}