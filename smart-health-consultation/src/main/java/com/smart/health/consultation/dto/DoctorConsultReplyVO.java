package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生回复结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "医生回复结果")
public class DoctorConsultReplyVO {

    @Schema(description = "本轮轮次号")
    private Integer turnNumber;

    @Schema(description = "回复后会话状态")
    private String sessionStatus;
}
