package com.smart.health.consultation.controller;

import com.smart.health.common.result.Result;
import com.smart.health.common.security.PatientUserDetails;
import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import com.smart.health.consultation.service.ConsultationService;
import com.smart.health.consultation.service.MultimodalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final ConsultationService consultationService;

    /**
     * 上传图片进行多模态分析
     */
    @PostMapping("/multimodal/analyze")
    @Operation(summary = "多模态图片分析", description = "上传图片（症状照片或检查报告），AI进行 multimodal 分析并返回结构化结果")
    public Result<MultimodalAnalyzeResponse> multimodalAnalyze(
            @Parameter(description = "上传图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "图片类型: IMAGE-症状图片, REPORT-检查报告") @RequestParam("type") String type,
            @AuthenticationPrincipal PatientUserDetails userDetails) {
        Long patientId = (userDetails != null) ? userDetails.getPatientId() : 0L;
        log.info("收到多模态分析请求, type={}, filename={}, patientId={}", type, file.getOriginalFilename(), patientId);
        MultimodalAnalyzeResponse response = multimodalService.analyze(file, type, patientId);
        return Result.ok(response);
    }

    /**
     * RAG 智能问诊流式追问 (SSE)
     * 基于 RAG + 知识图谱的智能全科医生问诊，采用 SSE 技术实现打字机流式输出
     */
    @PostMapping(value = "/consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式问诊", description = "基于RAG医学知识库的SSE流式问诊，支持多轮对话追问")
    public SseEmitter streamConsult(
            @RequestBody ConsultStreamRequest request,
            @AuthenticationPrincipal PatientUserDetails userDetails) {
        Long patientId = (userDetails != null) ? userDetails.getPatientId() : 0L;
        log.info("收到流式问诊请求, sessionId={}, patientId={}", request.getSessionId(), patientId);
        return consultationService.streamConsult(request, patientId);
    }
}
