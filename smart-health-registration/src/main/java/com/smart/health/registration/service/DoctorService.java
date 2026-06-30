package com.smart.health.registration.service;

import com.smart.health.registration.dto.DoctorVO;

/**
 * 医生信息服务接口
 */
public interface DoctorService {

    /**
     * 根据ID查询医生详情
     *
     * @param doctorId 医生ID
     * @return 医生信息VO，不存在时返回null
     */
    DoctorVO getDoctorById(Long doctorId);
}
