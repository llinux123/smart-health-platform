package com.smart.health.prescription.service.impl;

import com.smart.health.prescription.service.PdfGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 处方PDF存根生成服务实现
 * 当前为简化实现，生成文本存根文件；生产环境可替换为 iText / Apache PDFBox
 */
@Slf4j
@Service
public class PdfGenerationServiceImpl implements PdfGenerationService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Path storageDir;

    public PdfGenerationServiceImpl(
            @Value("${prescription.pdf.storage-dir:./prescription-pdfs}") String storageDirPath) {
        this.storageDir = Paths.get(storageDirPath);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.warn("无法创建PDF存储目录: {}", storageDirPath, e);
        }
    }

    @Override
    public String generatePrescriptionPdf(Long prescriptionId, String prescriptionSn,
                                           Long patientId, Long doctorId,
                                           String diagnosis) {
        String fileName = prescriptionSn + "_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".txt";
        Path filePath = storageDir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("===== 电子处方存根 =====");
            writer.newLine();
            writer.write("处方编号: " + prescriptionSn);
            writer.newLine();
            writer.write("处方ID: " + prescriptionId);
            writer.newLine();
            writer.write("患者ID: " + patientId);
            writer.newLine();
            writer.write("开具医生ID: " + doctorId);
            writer.newLine();
            writer.write("诊断结论: " + diagnosis);
            writer.newLine();
            writer.write("开具时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.newLine();
            writer.write("========================");
            writer.newLine();
            log.info("处方PDF存根已生成: {}", filePath);
        } catch (IOException e) {
            log.error("生成处方PDF存根失败: prescriptionSn={}", prescriptionSn, e);
            return null;
        }

        return filePath.toString();
    }
}
