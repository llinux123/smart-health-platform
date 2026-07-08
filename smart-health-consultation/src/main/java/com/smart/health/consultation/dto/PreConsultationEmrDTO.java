package com.smart.health.consultation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 结构化预问诊电子病历
 *
 * <p>由 LLM 基于患者对话历史提取，存储于 {@code ai_summary} 列的 JSON 中。</p>
 */
@Schema(description = "结构化预问诊电子病历")
public record PreConsultationEmrDTO(

        @Schema(description = "主诉 — 患者一句话概括不适")
        @JsonProperty("chiefComplaint")
        String chiefComplaint,

        @Schema(description = "现病史 — 发病过程、症状演变、持续时间等")
        @JsonProperty("presentIllness")
        String presentIllness,

        @Schema(description = "既往史 — 患者自述的慢性病/手术史等")
        @JsonProperty("pastHistory")
        String pastHistory,

        @Schema(description = "过敏史 — 药物/食物/其他过敏信息")
        @JsonProperty("allergyHistory")
        String allergyHistory,

        @Schema(description = "建议进一步检查项 — AI 基于分析给出的检查建议")
        @JsonProperty("suggestedExaminations")
        String suggestedExaminations
) {
}
