package com.smart.health.consultation.controller;

import com.smart.health.common.result.Result;
import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import com.smart.health.consultation.service.MultimodalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI问诊控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI问诊", description = "多模态图片分析、SSE流式对话等AI问诊接口")
public class AiConsultController {

    private final MultimodalService multimodalService;

    /**
     * 上传图片进行多模态分析
     */
    @PostMapping("/multimodal/analyze")
    @Operation(summary = "多模态图片分析", description = "上传图片（症状照片或检查报告），AI进行 multimodal 分析并返回结构化结果")
    public Result<MultimodalAnalyzeResponse> multimodalAnalyze(
            @Parameter(description = "上传图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "图片类型: IMAGE-症状图片, REPORT-检查报告") @RequestParam("type") String type) {
        log.info("收到多模态分析请求, type={}, filename={}", type, file.getOriginalFilename());
        MultimodalAnalyzeResponse response = multimodalService.analyze(file, type);
        return Result.ok(response);
    }
}
