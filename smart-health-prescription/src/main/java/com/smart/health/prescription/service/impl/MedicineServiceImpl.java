package com.smart.health.prescription.service.impl;

import com.smart.health.common.exception.BusinessException;
import com.smart.health.prescription.entity.Medicine;
import com.smart.health.prescription.mapper.MedicineMapper;
import com.smart.health.prescription.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 药品字典服务实现
 */
@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineMapper medicineMapper;

    @Override
    public List<Medicine> searchByKeyword(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return medicineMapper.selectList(0, limit);
        }
        return medicineMapper.selectByKeyword(keyword.trim(), limit);
    }

    @Override
    public Medicine getById(Long id) {
        Medicine medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        return medicine;
    }
}
