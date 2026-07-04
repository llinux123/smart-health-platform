package com.smart.health.prescription.mapper;

import com.smart.health.prescription.entity.Prescription;
import com.smart.health.prescription.enums.AuditStatus;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方Mapper
 */
@Mapper
public interface PrescriptionMapper {

    @Insert("INSERT INTO t_prescription (prescription_sn, patient_id, doctor_id, diagnosis, pdf_url, " +
            "audit_status, pharmacist_id, audit_comments, audit_time, status) " +
            "VALUES (#{prescriptionSn}, #{patientId}, #{doctorId}, #{diagnosis}, #{pdfUrl}, " +
            "#{auditStatus}, #{pharmacistId}, #{auditComments}, #{auditTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Prescription prescription);

    @Select("SELECT id, prescription_sn, patient_id, doctor_id, diagnosis, pdf_url, " +
            "audit_status, pharmacist_id, audit_comments, audit_time, status, create_time " +
            "FROM t_prescription WHERE id = #{id}")
    Prescription selectById(@Param("id") Long id);

    @Select("SELECT id, prescription_sn, patient_id, doctor_id, diagnosis, pdf_url, " +
            "audit_status, pharmacist_id, audit_comments, audit_time, status, create_time " +
            "FROM t_prescription WHERE prescription_sn = #{prescriptionSn}")
    Prescription selectByPrescriptionSn(@Param("prescriptionSn") String prescriptionSn);

    @Select("SELECT id, prescription_sn, patient_id, doctor_id, diagnosis, pdf_url, " +
            "audit_status, pharmacist_id, audit_comments, audit_time, status, create_time " +
            "FROM t_prescription WHERE patient_id = #{patientId} ORDER BY create_time DESC")
    List<Prescription> selectByPatientId(@Param("patientId") Long patientId);

    @Update("UPDATE t_prescription SET audit_status = #{auditStatus}, pharmacist_id = #{pharmacistId}, " +
            "audit_comments = #{auditComments}, audit_time = #{auditTime} WHERE id = #{id}")
    int updateAuditStatus(@Param("id") Long id,
                          @Param("auditStatus") AuditStatus auditStatus,
                          @Param("pharmacistId") Long pharmacistId,
                          @Param("auditComments") String auditComments,
                          @Param("auditTime") LocalDateTime auditTime);

    @Update("UPDATE t_prescription SET pdf_url = #{pdfUrl} WHERE id = #{id}")
    int updatePdfUrl(@Param("id") Long id, @Param("pdfUrl") String pdfUrl);

    @Select("SELECT id, prescription_sn, patient_id, doctor_id, diagnosis, pdf_url, " +
            "audit_status, pharmacist_id, audit_comments, audit_time, status, create_time " +
            "FROM t_prescription WHERE audit_status = #{auditStatus} ORDER BY create_time ASC")
    List<Prescription> selectByAuditStatus(@Param("auditStatus") AuditStatus auditStatus);

    @Select("SELECT COUNT(*) FROM t_prescription WHERE patient_id = #{patientId}")
    int countByPatientId(@Param("patientId") Long patientId);
}
