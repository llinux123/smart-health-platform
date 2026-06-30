package com.smart.health.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 医生信息视图对象
 */
@Data
@Schema(description = "医生信息")
public class DoctorVO {

    @Schema(description = "医生ID")
    private Long id;

    @Schema(description = "医生姓名")
    private String name;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "所属科室")
    private String deptName;

    @Schema(description = "擅长领域")
    private String specialty;

    @Schema(description = "医生简介")
    private String intro;
}
