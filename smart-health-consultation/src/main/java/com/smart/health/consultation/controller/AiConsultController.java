package com.smart.health.consultation.controller;

import com.smart.health.common.constant.CommonConstants;
import com.smart.health.common.result.PageResult;
import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.Result;
import com.smart.health.common.security.SecurityUtils;
import com.smart.health.consultation.dto.*;
import com.smart.health.consultation.service.ChatStream;
import com.smart.health.consultation.service.MultimodalService;
import com.smart.health.consultation.service.SessionArchive;
import com.smart.health.consultation.service.SessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI问诊控制器 — 拆分后仅负责请求转发，不包含业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI问诊", description = "多模态图片分析、SSE流式对话、会话管理等AI问诊接口")
@PreAuthorize("hasRole('PATIENT')")
public class AiConsultController {

    private final MultimodalService multimodalService;
    private final ChatStream chatStream;
    private final SessionManager sessionManager;
    private final SessionArchive sessionArchive;

    // ============ 多模态接口 ============

    @PostMapping("/multimodal/analyze")
    @Operation(summary = "多模态图片分析")
    public Result<MultimodalAnalyzeResponse> multimodalAnalyze(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        MultimodalAnalyzeResponse response = multimodalService.analyze(files, type, patientId);
        return Result.ok(response);
    }

    // ============ SSE 流式问诊 ============

    @PostMapping(value = "/consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式问诊")
    public SseEmitter streamConsult(
            @RequestBody ConsultStreamRequest request,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        return chatStream.streamConsult(request, patientId);
    }

    // ============ 会话管理（SessionManager） ============

    @PostMapping("/sessions")
    @Operation(summary = "创建问诊会话")
    public Result<String> createSession(
            @RequestParam(value = "draftId", required = false) String draftId,
            @RequestParam(value = "symptomDraft", required = false) String symptomDraft,
            @RequestParam(value = "fileUrls", required = false) String fileUrls,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        String sessionSn = sessionManager.createSession(patientId, draftId, symptomDraft, fileUrls);
        return Result.ok(sessionSn);
    }

    @GetMapping("/sessions")
    @Operation(summary = "问诊会话列表", description = "分页查询，支持搜索、筛选、置顶排序")
    public Result<PageResult<SessionVO>> listSessions(
            SessionListRequest request,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        PageResult<SessionVO> result = sessionManager.listSessions(patientId, request);
        return Result.ok(result);
    }

    @GetMapping("/sessions/{sessionSn}")
    @Operation(summary = "会话详情")
    public Result<SessionVO> getSessionDetail(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        SessionVO result = sessionManager.getSessionDetail(sessionSn, patientId);
        return Result.ok(result);
    }

    @GetMapping("/sessions/{sessionSn}/turns")
    @Operation(summary = "对话轮次列表", description = "分页获取，按 turn_number DESC")
    public Result<PageResult<TurnVO>> getSessionTurns(
            @PathVariable String sessionSn,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "5") int size,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        PageResult<TurnVO> result = sessionManager.getSessionTurns(sessionSn, patientId, page, size);
        return Result.ok(result);
    }

    @PostMapping("/sessions/{sessionSn}/complete")
    @Operation(summary = "确认结束问诊")
    public Result<Void> completeSession(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionManager.completeSession(sessionSn, patientId);
        return Result.ok();
    }

    @PutMapping("/sessions/{sessionSn}/pin")
    @Operation(summary = "置顶/取消置顶")
    public Result<Void> togglePin(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionManager.togglePin(sessionSn, patientId);
        return Result.ok();
    }

    // ============ 转诊 ============

    @PostMapping("/sessions/{sessionSn}/handoff")
    @Operation(summary = "转接真人医生", description = "患者将AI问诊会话转接给真人医生")
    public Result<Void> handoffSession(
            @PathVariable String sessionSn,
            @Parameter(description = "转诊原因（可选）") @RequestParam(required = false) String reason,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionManager.handoffSession(sessionSn, patientId, reason);
        return Result.ok();
    }

    // ============ 重新生成（ChatStream） ============

    @PostMapping("/sessions/{sessionSn}/turns/{turnNumber}/regenerate")
    @Operation(summary = "重新生成AI回复", description = "仅支持最后一轮")
    public Result<TurnVO> regenerateLastTurn(
            @PathVariable String sessionSn,
            @PathVariable Integer turnNumber,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        TurnVO result = chatStream.regenerateLastTurn(sessionSn, patientId, turnNumber);
        return Result.ok(result);
    }

    // ============ 删除与回收站（SessionArchive） ============

    @PostMapping("/sessions/{sessionSn}/delete")
    @Operation(summary = "删除会话", description = "mode=recycle移入回收站, mode=permanent彻底删除")
    public Result<Void> deleteSession(
            @PathVariable String sessionSn,
            @Parameter(description = "删除模式: recycle/permanent") @RequestParam String mode,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionArchive.deleteSession(sessionSn, patientId, mode);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionSn}/rate")
    @Operation(summary = "问诊评分")
    public Result<Void> rateSession(
            @PathVariable String sessionSn,
            @Valid @RequestBody RatingRequest request,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionArchive.rateSession(sessionSn, patientId, request);
        return Result.ok();
    }

    @GetMapping("/sessions/recycle-bin")
    @Operation(summary = "回收站列表")
    public Result<PageResult<SessionVO>> listRecycleBin(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        PageResult<SessionVO> result = sessionArchive.listRecycleBin(patientId, page, size);
        return Result.ok(result);
    }

    @PostMapping("/sessions/recycle-bin/{sessionSn}/restore")
    @Operation(summary = "从回收站恢复")
    public Result<Void> restoreSession(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionArchive.restoreSession(sessionSn, patientId);
        return Result.ok();
    }

    @PostMapping("/sessions/recycle-bin/{sessionSn}/permanent")
    @Operation(summary = "回收站中彻底删除")
    public Result<Void> permanentDeleteFromRecycleBin(
            @PathVariable String sessionSn,
            @RequestHeader(value = CommonConstants.TOKEN_HEADER, required = false) String bearerToken) {
        Long patientId = extractPatientId(bearerToken);
        sessionArchive.permanentDeleteFromRecycleBin(sessionSn, patientId);
        return Result.ok();
    }

    // ============ 工具方法 ============

    private Long extractPatientId(String bearerToken) {
        Long patientId = SecurityUtils.tryGetCurrentPatientId();
        if (patientId == null) {
            throw new BusinessException("请先登录");
        }
        return patientId;
    }
}
