package com.smart.health.registration.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.registration.entity.Department;
import com.smart.health.registration.mapper.DepartmentMapper;
import com.smart.health.registration.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 科室管理服务实现
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public List<Department> listActiveDepartments() {
        return departmentMapper.selectActive();
    }

    @Override
    public Department getDepartmentById(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("科室不存在");
        }
        return dept;
    }
}
