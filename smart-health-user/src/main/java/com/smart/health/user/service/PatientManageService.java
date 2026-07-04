package com.smart.health.user.service;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.common.result.ResultCode;
import com.smart.health.user.entity.Patient;
import com.smart.health.user.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 患者管理服务 — ADMIN 端 CRUD 操作
 */
@Service
@RequiredArgsConstructor
public class PatientManageService {

    private final PatientMapper patientMapper;

    /**
     * 查询全部患者列表
     */
    public List<Patient> listAll() {
        return patientMapper.findAll();
    }

    /**
     * 根据 ID 查询患者详情
     *
     * @throws BusinessException 患者不存在时抛出
     */
    public Patient getById(Long id) {
        Patient patient = patientMapper.findById(id);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return patient;
    }

    /**
     * 更新患者信息
     *
     * @throws BusinessException 患者不存在时抛出
     */
    public void update(Long id, Patient patient) {
        Patient existing = patientMapper.findById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        patient.setId(id);
        patientMapper.update(patient);
    }

    /**
     * 删除患者（软删除）
     *
     * @throws BusinessException 患者不存在时抛出
     */
    public void delete(Long id) {
        Patient patient = patientMapper.findById(id);
        if (patient == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        patientMapper.softDelete(id);
    }
}
