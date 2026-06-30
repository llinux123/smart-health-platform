package com.smart.health.registration.service.impl;

import com.smart.health.registration.dto.DoctorVO;
import com.smart.health.registration.entity.Doctor;
import com.smart.health.registration.mapper.DoctorMapper;
import com.smart.health.registration.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 医生信息服务实现
 */
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;

    @Override
    public DoctorVO getDoctorById(Long doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) {
            return null;
        }
        return toVO(doctor);
    }

    /**
     * 实体转 VO
     */
    private DoctorVO toVO(Doctor doctor) {
        DoctorVO vo = new DoctorVO();
        vo.setId(doctor.getId());
        vo.setName(doctor.getName());
        vo.setTitle(doctor.getTitle());
        vo.setAvatar(doctor.getAvatar());
        vo.setDeptName(doctor.getDeptName());
        vo.setSpecialty(doctor.getSpecialty());
        vo.setIntro(doctor.getIntro());
        return vo;
    }
}
