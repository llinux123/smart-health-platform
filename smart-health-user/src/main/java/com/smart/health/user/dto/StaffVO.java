package com.smart.health.user.dto;

import com.smart.health.user.entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工信息视图对象（不暴露密码）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String role;
    private Long doctorId;
    private LocalDateTime createTime;

    public static StaffVO from(Staff staff) {
        return StaffVO.builder()
                .id(staff.getId())
                .username(staff.getUsername())
                .realName(staff.getRealName())
                .phone(staff.getPhone())
                .role(staff.getRole())
                .doctorId(staff.getDoctorId())
                .createTime(staff.getCreateTime())
                .build();
    }
}
