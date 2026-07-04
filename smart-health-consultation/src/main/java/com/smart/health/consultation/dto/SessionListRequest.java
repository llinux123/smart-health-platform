package com.smart.health.consultation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话列表查询参数
 */
@Data
@Schema(description = "会话列表查询参数")
public class SessionListRequest {

    @Schema(description = "搜索关键词(匹配症状描述或AI总结)")
    private String keyword;

    @Schema(description = "开始日期(yyyy-MM-dd)")
    private String startDate;

    @Schema(description = "结束日期(yyyy-MM-dd)")
    private String endDate;

    @Schema(description = "会话状态筛选", allowableValues = {"IN_PROGRESS", "COMPLETED"})
    private String status;

    @Schema(description = "是否置顶")
    private Boolean isPinned;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
