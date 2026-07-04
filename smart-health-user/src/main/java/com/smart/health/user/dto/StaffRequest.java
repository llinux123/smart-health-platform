package com.smart.health.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建/编辑员工请求DTO
 */
@Data
public class StaffRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新建时必填，编辑时可不传（不修改密码） */
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "DOCTOR|PHARMACIST|ADMIN", message = "角色只能为 DOCTOR / PHARMACIST / ADMIN")
    private String role;

    /** 仅 DOCTOR 角色时传入，关联 t_doctor.id */
    private Long doctorId;
}
