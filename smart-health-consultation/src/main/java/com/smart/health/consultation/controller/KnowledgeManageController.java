package com.smart.health.consultation.controller;

import com.smart.health.common.result.PageResult;
import com.smart.health.common.result.Result;
import com.smart.health.consultation.dto.KnowledgeImportRequest;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import com.smart.health.consultation.service.RagRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库管理控制器 — 仅 ADMIN 角色可访问
 * 提供知识库文档的导入、分页查询、删除功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "医学知识库的导入、查询与删除（仅管理员）")
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeManageController {

    private final RagRetrievalService ragRetrievalService;

    @PostMapping("/import")
    @Operation(summary = "导入医学知识")
    public Result<Integer> importKnowledge(@RequestBody KnowledgeImportRequest request) {
        int count = ragRetrievalService.importDocument(
                request.getTitle(), request.getContent(), request.getCategory());
        return Result.ok(count);
    }

    @GetMapping
    @Operation(summary = "分页查询知识库", description = "支持关键字搜索（匹配标题或内容）")
    public Result<PageResult<MedicalKnowledgeDocument>> listKnowledge(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        PageResult<MedicalKnowledgeDocument> result = ragRetrievalService.listDocuments(page, size, keyword);
        return Result.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库文档")
    public Result<Void> deleteKnowledge(@PathVariable String id) {
        boolean success = ragRetrievalService.deleteDocument(id);
        if (!success) {
            return Result.fail("删除失败，文档可能不存在");
        }
        return Result.ok();
    }
}
