package com.smart.health.consultation.controller;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.result.Result;
import com.smart.health.consultation.dto.ConsultStreamRequest;
import com.smart.health.consultation.dto.KnowledgeImportRequest;
import com.smart.health.consultation.dto.MultimodalAnalyzeResponse;
import com.smart.health.consultation.dto.SessionHistoryVO;
import com.smart.health.consultation.dto.SessionVO;
import com.smart.health.consultation.service.ConsultationService;
import com.smart.health.consultation.service.MultimodalService;
import com.smart.health.consultation.service.RagRetrievalService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

/**
 * AI问诊控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI问诊", description = "多模态图片分析、SSE流式对话、会话管理等AI问诊接口")
public class AiConsultController {

    private final MultimodalService multimodalService;
    private final ConsultationService consultationService;
    private final RagRetrievalService ragRetrievalService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 上传图片进行多模态分析
     */
    @PostMapping("/multimodal/analyze")
    @Operation(summary = "多模态图片分析", description = "上传图片（症状照片或检查报告），AI进行 multimodal 分析并返回结构化结果")
    public Result<MultimodalAnalyzeResponse> multimodalAnalyze(
            @Parameter(description = "上传图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "图片类型: IMAGE-症状图片, REPORT-检查报告") @RequestParam("type") String type,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        log.info("收到多模态分析请求, type={}, filename={}, patientId={}", type, file.getOriginalFilename(), patientId);
        MultimodalAnalyzeResponse response = multimodalService.analyze(file, type, patientId);
        return Result.ok(response);
    }

    /**
     * RAG 智能问诊流式追问 (SSE)
     * 基于 RAG + 知识图谱的智能全科医生问诊，采用 SSE 技术实现打字机流式输出
     */
    @PostMapping(value = "/consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式问诊", description = "基于RAG医学知识库的SSE流式问诊，支持多轮对话追问，返回引用来源")
    public SseEmitter streamConsult(
            @RequestBody ConsultStreamRequest request,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        log.info("收到流式问诊请求, sessionId={}, patientId={}", request.getSessionId(), patientId);
        return consultationService.streamConsult(request, patientId);
    }

    /**
     * 创建新的问诊会话
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建问诊会话", description = "创建新的问诊会话，可选关联多模态分析草稿")
    public Result<String> createSession(
            @RequestParam(value = "draftId", required = false) String draftId,
            @RequestParam(value = "symptomDraft", required = false) String symptomDraft,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        String sessionSn = consultationService.createSession(patientId, draftId, symptomDraft);
        log.info("创建问诊会话, sessionSn={}, patientId={}", sessionSn, patientId);
        return Result.ok(sessionSn);
    }

    /**
     * 查询患者的问诊会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "问诊会话列表", description = "查询当前患者的所有问诊会话")
    public Result<List<SessionVO>> listSessions(
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        List<SessionVO> sessions = consultationService.listSessions(patientId);
        return Result.ok(sessions);
    }

    /**
     * 获取会话的完整对话历史
     */
    @GetMapping("/sessions/{sessionSn}/history")
    @Operation(summary = "会话对话历史", description = "获取指定会话的完整多轮对话记录")
    public Result<List<SessionHistoryVO>> getSessionHistory(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        List<SessionHistoryVO> history = consultationService.getSessionHistory(sessionSn, patientId);
        return Result.ok(history);
    }

    /**
     * 导入医学知识文档到 ES 知识库
     */
    @PostMapping("/knowledge/import")
    @Operation(summary = "导入医学知识", description = "导入医学知识文档到 ES 知识库，自动生成 Embedding 向量")
    public Result<Integer> importKnowledge(@RequestBody KnowledgeImportRequest request) {
        int count = ragRetrievalService.importDocument(
                request.getTitle(), request.getContent(), request.getCategory());
        return Result.ok(count);
    }
    
    /**
     * 从 JWT token 中提取patientId
     */
    private Long extractPatientId(String bearerToken) {
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return 0L;
        }
        try {
            String token = bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("patientId", Long.class);
        } catch (Exception e) {
            log.warn("从JWT提取patientId失败: {}", e.getMessage());
            return 0L;
        }
    }
}
