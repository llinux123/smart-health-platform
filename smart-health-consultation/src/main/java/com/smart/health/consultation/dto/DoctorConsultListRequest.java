package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医生端待接诊列表查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "医生端待接诊列表查询参数")
public class DoctorConsultListRequest {

    @Schema(description = "搜索关键字（匹配症状描述/AI总结）")
    private String keyword;

    @Schema(description = "页码", defaultValue = "1")
    private int page = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private int size = 10;
}
