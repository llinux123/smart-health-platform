package com.smart.health.consultation.mapper;

import com.smart.health.consultation.entity.ConsultationSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI问诊会话Mapper
 */
@Mapper
public interface ConsultationSessionMapper {

    /**
     * 插入问诊会话记录
     */
    @Insert("INSERT INTO t_consultation_session (session_sn, patient_id, draft_id, symptom_draft, chat_log) " +
            "VALUES (#{sessionSn}, #{patientId}, #{draftId}, #{symptomDraft}, #{chatLog})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationSession session);

    /**
     * 根据ID查询
     */
    @Select("SELECT id, session_sn, patient_id, draft_id, symptom_draft, chat_log, create_time, update_time " +
            "FROM t_consultation_session WHERE id = #{id}")
    ConsultationSession selectById(@Param("id") Long id);

    /**
     * 根据会话编号查询
     */
    @Select("SELECT id, session_sn, patient_id, draft_id, symptom_draft, chat_log, create_time, update_time " +
            "FROM t_consultation_session WHERE session_sn = #{sessionSn}")
    ConsultationSession selectBySessionSn(@Param("sessionSn") String sessionSn);

    /**
     * 查询患者的所有问诊会话（按创建时间倒序）
     */
    @Select("SELECT id, session_sn, patient_id, draft_id, symptom_draft, chat_log, create_time, update_time " +
            "FROM t_consultation_session WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<ConsultationSession> selectByPatientId(@Param("patientId") Long patientId);

    /**
     * 更新对话日志
     */
    @Update("UPDATE t_consultation_session SET chat_log = #{chatLog} WHERE id = #{id}")
    int updateChatLog(@Param("id") Long id, @Param("chatLog") String chatLog);

    /**
     * 更新草稿信息
     */
    @Update("UPDATE t_consultation_session SET draft_id = #{draftId}, symptom_draft = #{symptomDraft} WHERE id = #{id}")
    int updateDraft(@Param("id") Long id, @Param("draftId") String draftId, @Param("symptomDraft") String symptomDraft);
}
