package com.smart.health.consultation.controller;

import com.smart.health.common.result.PageResult;
import com.smart.health.common.result.Result;
import com.smart.health.consultation.dto.KnowledgeImportRequest;
import com.smart.health.consultation.dto.KnowledgeUpdateRequest;
import com.smart.health.consultation.entity.MedicalKnowledgeDocument;
import com.smart.health.consultation.service.RagRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器 — 仅 ADMIN 角色可访问
 * 提供知识库文档的导入、分页查询、详情、编辑、删除、分类列表
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "医学知识库的导入、查询、编辑、删除与分类聚合（仅管理员）")
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeManageController {

    private final RagRetrievalService ragRetrievalService;

    @PostMapping("/import")
    @Operation(summary = "导入医学知识")
    public Result<Integer> importKnowledge(@Valid @RequestBody KnowledgeImportRequest request) {
        int count = ragRetrievalService.importDocument(
                request.getTitle(), request.getContent(), request.getCategory());
        return Result.ok(count);
    }

    @GetMapping
    @Operation(summary = "分页查询知识库", description = "支持关键字搜索（匹配标题或内容）和分类过滤，按更新时间倒序")
    public Result<PageResult<MedicalKnowledgeDocument>> listKnowledge(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类过滤") @RequestParam(required = false) String category) {
        PageResult<MedicalKnowledgeDocument> result = ragRetrievalService.listDocuments(page, size, keyword, category);
        return Result.ok(result);
    }

    @GetMapping("/categories")
    @Operation(summary = "知识库分类列表", description = "从 ES 动态聚合所有出现过的 category 名称，按字典序返回")
    public Result<List<String>> listCategories() {
        return Result.ok(ragRetrievalService.listCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库文档详情")
    public Result<MedicalKnowledgeDocument> getKnowledge(@PathVariable String id) {
        MedicalKnowledgeDocument doc = ragRetrievalService.getDocument(id);
        if (doc == null) {
            return Result.fail("文档不存在或已被删除");
        }
        return Result.ok(doc);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库文档", description = "全量覆盖 title/content/category，title 或 content 变化时重新生成 embedding")
    public Result<MedicalKnowledgeDocument> updateKnowledge(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeUpdateRequest request) {
        MedicalKnowledgeDocument doc = ragRetrievalService.updateDocument(
                id, request.getTitle(), request.getContent(), request.getCategory());
        if (doc == null) {
            return Result.fail("文档不存在或已被删除");
        }
        return Result.ok(doc);
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