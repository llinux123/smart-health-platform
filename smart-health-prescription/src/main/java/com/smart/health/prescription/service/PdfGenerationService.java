package com.smart.health.prescription.service;

/**
 * 处方PDF存根生成服务
 */
public interface PdfGenerationService {

    /**
     * 生成处方PDF存根并保存到本地
     *
     * @param prescriptionId 处方ID
     * @param prescriptionSn 处方编号
     * @param patientId      患者ID
     * @param doctorId       医生ID
     * @param diagnosis      诊断结论
     * @return PDF文件存储路径
     */
    String generatePrescriptionPdf(Long prescriptionId, String prescriptionSn,
                                    Long patientId, Long doctorId,
                                    String diagnosis);
}
