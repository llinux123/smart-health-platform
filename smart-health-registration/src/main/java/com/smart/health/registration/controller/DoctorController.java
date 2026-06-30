package com.smart.health.registration.controller;

import com.smart.health.common.result.Result;
import com.smart.health.common.result.ResultCode;
import com.smart.health.registration.dto.DoctorVO;
import com.smart.health.registration.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生信息控制器
 */
@Tag(name = "医生信息", description = "医生详情查询")
@RestController
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(summary = "查询医生详情")
    @GetMapping("/api/v1/doctor/{id}")
    public ResponseEntity<Result<DoctorVO>> getDoctorDetail(@PathVariable Long id) {
        DoctorVO doctor = doctorService.getDoctorById(id);
        if (doctor == null) {
            return ResponseEntity.status(404)
                    .body(Result.fail(ResultCode.DOCTOR_NOT_FOUND));
        }
        return ResponseEntity.ok(Result.ok(doctor));
    }
}
