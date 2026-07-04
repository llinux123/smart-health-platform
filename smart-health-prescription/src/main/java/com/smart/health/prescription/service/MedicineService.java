package com.smart.health.prescription.service;

import com.smart.health.prescription.entity.Medicine;

import java.util.List;

/**
 * 药品字典服务接口
 */
public interface MedicineService {

    /**
     * 按关键字搜索药品（前缀优先 + 模糊匹配）
     */
    List<Medicine> searchByKeyword(String keyword, int limit);

    /**
     * 根据ID查询药品
     */
    Medicine getById(Long id);
}
