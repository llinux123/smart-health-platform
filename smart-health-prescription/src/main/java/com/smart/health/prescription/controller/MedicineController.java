package com.smart.health.prescription.controller;

import com.smart.health.common.result.Result;
import com.smart.health.prescription.entity.Medicine;
import com.smart.health.prescription.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 药品字典控制器
 */
@Tag(name = "药品字典", description = "药品搜索与查询")
@RestController
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @Operation(summary = "搜索药品（前缀优先 + 模糊匹配）")
    @GetMapping("/api/v1/medicine/search")
    public Result<List<Medicine>> search(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "返回条数上限") @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(medicineService.searchByKeyword(keyword, limit));
    }

    @Operation(summary = "获取药品详情")
    @GetMapping("/api/v1/medicine/{id}")
    public Result<Medicine> detail(@PathVariable Long id) {
        return Result.ok(medicineService.getById(id));
    }
}
