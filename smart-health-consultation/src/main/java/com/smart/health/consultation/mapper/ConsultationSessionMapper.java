package com.smart.health.consultation.mapper;

import com.smart.health.consultation.entity.ConsultationSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI问诊会话Mapper
 */
@Mapper
public interface ConsultationSessionMapper {

    @Insert("INSERT INTO t_consultation_session (session_sn, patient_id, draft_id, symptom_draft, file_urls, status, last_chat_time) " +
            "VALUES (#{sessionSn}, #{patientId}, #{draftId}, #{symptomDraft}, #{fileUrls}, #{status}, #{lastChatTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConsultationSession session);

    @Select("SELECT * FROM t_consultation_session WHERE id = #{id} AND is_deleted = 0")
    ConsultationSession selectById(@Param("id") Long id);

    @Select("SELECT * FROM t_consultation_session WHERE session_sn = #{sessionSn} AND is_deleted = 0")
    ConsultationSession selectBySessionSn(@Param("sessionSn") String sessionSn);

    @Select("SELECT * FROM t_consultation_session WHERE session_sn = #{sessionSn}")
    ConsultationSession selectBySessionSnIncludeDeleted(@Param("sessionSn") String sessionSn);

    @Select({
            "<script>",
            "SELECT * FROM t_consultation_session",
            "WHERE patient_id = #{patientId} AND is_deleted = 0",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (symptom_draft LIKE CONCAT('%', #{keyword}, '%')",
            "       OR ai_summary LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "  AND status = #{status}",
            "</if>",
            "<if test='isPinned != null'>",
            "  AND is_pinned = #{isPinned}",
            "</if>",
            "<if test='startDate != null and startDate != \"\"'>",
            "  AND last_chat_time &gt;= #{startDate}",
            "</if>",
            "<if test='endDate != null and endDate != \"\"'>",
            "  AND last_chat_time &lt;= #{endDate}",
            "</if>",
            "ORDER BY is_pinned DESC, last_chat_time DESC",
            "</script>"
    })
    List<ConsultationSession> selectByPatientIdWithFilter(
            @Param("patientId") Long patientId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("isPinned") Boolean isPinned,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    @Select("SELECT * FROM t_consultation_session WHERE patient_id = #{patientId} AND is_deleted = 1 ORDER BY deleted_at DESC")
    List<ConsultationSession> selectRecycleBinByPatientId(@Param("patientId") Long patientId);

    @Update("UPDATE t_consultation_session SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE t_consultation_session SET is_pinned = NOT is_pinned, update_time = NOW() WHERE id = #{id}")
    int togglePin(@Param("id") Long id);

    @Update("UPDATE t_consultation_session SET is_deleted = 1, deleted_at = NOW(), update_time = NOW() WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE t_consultation_session SET is_deleted = 0, deleted_at = NULL, update_time = NOW() WHERE id = #{id}")
    int restoreFromRecycleBin(@Param("id") Long id);

    @Delete("DELETE FROM t_consultation_session WHERE id = #{id}")
    int physicalDelete(@Param("id") Long id);

    @Update("UPDATE t_consultation_session SET last_chat_time = #{lastChatTime}, update_time = NOW() WHERE id = #{id}")
    int updateLastChatTime(@Param("id") Long id, @Param("lastChatTime") LocalDateTime lastChatTime);

    @Update("UPDATE t_consultation_session SET ai_summary = #{aiSummary}, update_time = NOW() WHERE id = #{id}")
    int updateAiSummary(@Param("id") Long id, @Param("aiSummary") String aiSummary);

    @Select("SELECT id FROM t_consultation_session WHERE is_deleted = 1 AND deleted_at < #{beforeTime}")
    List<Long> selectExpiredRecycleBinItems(@Param("beforeTime") LocalDateTime beforeTime);

    @Update("UPDATE t_consultation_session SET draft_id = #{draftId}, symptom_draft = #{symptomDraft} WHERE id = #{id}")
    int updateDraft(@Param("id") Long id, @Param("draftId") String draftId, @Param("symptomDraft") String symptomDraft);

    @Select("SELECT COUNT(*) FROM t_consultation_session WHERE patient_id = #{patientId} AND is_deleted = 0")
    int countByPatientId(@Param("patientId") Long patientId);

    /**
     * 查询医生待接诊/沟通中的会话（状态为 PENDING_DOCTOR 或 DOCTOR_ACTIVE）
     */
    @Select({
            "<script>",
            "SELECT cs.* FROM t_consultation_session cs",
            "WHERE cs.is_deleted = 0",
            "  AND cs.status IN ('PENDING_DOCTOR', 'DOCTOR_ACTIVE')",
            "  AND (cs.assigned_doctor_id IS NULL OR cs.assigned_doctor_id = #{doctorId})",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (cs.symptom_draft LIKE CONCAT('%', #{keyword}, '%')",
            "       OR cs.ai_summary LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY FIELD(cs.status, 'PENDING_DOCTOR', 'DOCTOR_ACTIVE'), cs.last_chat_time DESC",
            "</script>"
    })
    List<ConsultationSession> selectPendingForDoctor(@Param("doctorId") Long doctorId,
                                                     @Param("keyword") String keyword);

    /**
     * 更新会话状态 + 指配医生
     */
    @Update("UPDATE t_consultation_session SET status = #{status}, assigned_doctor_id = #{doctorId}, update_time = NOW() WHERE id = #{id}")
    int updateStatusAndDoctor(@Param("id") Long id, @Param("status") String status, @Param("doctorId") Long doctorId);
}
