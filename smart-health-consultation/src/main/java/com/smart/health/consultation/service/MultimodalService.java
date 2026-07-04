package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 多模态图片分析服务接口
 */
public interface MultimodalService {

    /**
     * 上传多文件进行多模态分析
     *
     * @param files 上传的文件列表（图片/PDF/Word）
     * @param type  文件类型（IMAGE-症状图片, REPORT-检查报告）
     * @param patientId 当前登录患者ID
     * @return 分析结果
     */
    MultimodalAnalyzeResponse analyze(List<MultipartFile> files, String type, Long patientId);
}
