package com.smart.health.user.mapper;

import com.smart.health.user.entity.Patient;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 患者用户Mapper
 */
@Mapper
public interface PatientMapper {

    @Insert("INSERT INTO t_patient (username, password, real_name, id_card, phone, gender, email, avatar, " +
            "birthday, id_card_status, id_card_front_url, id_card_back_url, face_recognition_url) " +
            "VALUES (#{username}, #{password}, #{realName}, #{idCard}, #{phone}, #{gender}, #{email}, #{avatar}, " +
            "#{birthday}, #{idCardStatus}, #{idCardFrontUrl}, #{idCardBackUrl}, #{faceRecognitionUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Patient patient);

    @Select("SELECT * FROM t_patient WHERE username = #{username} AND is_deleted = 0")
    Patient findByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_patient WHERE id = #{id} AND is_deleted = 0")
    Patient findById(@Param("id") Long id);

    @Select("SELECT * FROM t_patient WHERE phone = #{phone} AND is_deleted = 0")
    Patient findByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(*) FROM t_patient WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM t_patient WHERE id_card = #{idCard} AND is_deleted = 0")
    int countByIdCard(@Param("idCard") String idCard);

    @Select("SELECT COUNT(*) FROM t_patient WHERE phone = #{phone} AND is_deleted = 0")
    int countByPhone(@Param("phone") String phone);

    // ==================== 管理员专用 ====================

    @Select("SELECT * FROM t_patient WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<Patient> findAll();

    @Update({"<script>",
            "UPDATE t_patient SET",
            "  real_name = #{realName},",
            "  id_card = #{idCard},",
            "  phone = #{phone},",
            "  gender = #{gender},",
            "  email = #{email},",
            "  avatar = #{avatar},",
            "  birthday = #{birthday},",
            "  id_card_status = #{idCardStatus},",
            "  id_card_front_url = #{idCardFrontUrl},",
            "  id_card_back_url = #{idCardBackUrl},",
            "  face_recognition_url = #{faceRecognitionUrl}",
            "WHERE id = #{id} AND is_deleted = 0",
            "</script>"})
    int update(Patient patient);

    @Update("UPDATE t_patient SET password = #{password} WHERE phone = #{phone} AND is_deleted = 0")
    int updatePasswordByPhone(@Param("phone") String phone, @Param("password") String password);

    @Update("UPDATE t_patient SET is_deleted = 1, deleted_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);
}
