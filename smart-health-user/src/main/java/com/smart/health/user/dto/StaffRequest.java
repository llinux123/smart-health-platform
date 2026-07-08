package com.smart.health.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑员工请求DTO
 */
@Data
public class StaffRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5-]+$", message = "用户名仅支持中英文、数字、下划线和连字符")
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
