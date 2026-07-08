package com.smart.health.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新头像请求DTO
 */
@Data
public class UpdateAvatarRequest {

    @NotBlank(message = "头像 URL 不能为空")
    private String avatarUrl;
}
