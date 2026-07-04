package com.smart.health.registration.service;

import com.smart.health.registration.entity.Department;

import java.util.List;

/**
 * 科室管理服务接口
 */
public interface DepartmentService {

    /**
     * 查询启用的科室列表
     */
    List<Department> listActiveDepartments();

    /**
     * 根据ID查询科室
     */
    Department getDepartmentById(Long id);
}
