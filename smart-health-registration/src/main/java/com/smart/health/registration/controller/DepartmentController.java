package com.smart.health.registration.controller;

import com.smart.health.common.result.Result;
import com.smart.health.registration.entity.Department;
import com.smart.health.registration.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 科室信息控制器
 */
@Tag(name = "科室管理", description = "科室信息查询")
@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "获取启用科室列表")
    @GetMapping("/api/v1/dept/list")
    public Result<List<Department>> list() {
        return Result.ok(departmentService.listActiveDepartments());
    }

    @Operation(summary = "获取科室详情")
    @GetMapping("/api/v1/dept/{id}")
    public Result<Department> detail(@PathVariable Long id) {
        return Result.ok(departmentService.getDepartmentById(id));
    }
}
