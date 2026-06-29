package com.smart.health.consultation.service;

import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 多模态图片分析服务接口
 */
public interface MultimodalService {

    /**
     * 上传图片进行多模态分析
     *
     * @param file 上传的图片文件
     * @param type 图片类型（IMAGE-症状图片, REPORT-检查报告）
     * @param patientId 当前登录患者ID
     * @return 分析结果
     */
    MultimodalAnalyzeResponse analyze(MultipartFile file, String type, Long patientId);
}
