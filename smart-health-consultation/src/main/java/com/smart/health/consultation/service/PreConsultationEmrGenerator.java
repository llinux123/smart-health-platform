package com.smart.health.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.dto.PreConsultationEmrDTO;
import com.smart.health.consultation.entity.ConsultationTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 结构化预问诊电子病历生成器
 *
 * <p>基于问诊对话历史，调用 LLM 提取 5 字段结构化病历。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreConsultationEmrGenerator {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * 生成结构化预问诊电子病历
     *
     * @param turns       对话轮次（按时间升序）
     * @param symptomDraft 患者症状草稿（可选）
     * @return 结构化电子病历
     */
    public PreConsultationEmrDTO generate(List<ConsultationTurn> turns, String symptomDraft) {
        BeanOutputConverter<PreConsultationEmrDTO> converter = new BeanOutputConverter<>(PreConsultationEmrDTO.class);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt()));

        StringBuilder userContent = new StringBuilder();
        if (symptomDraft != null && !symptomDraft.isBlank()) {
            userContent.append("【患者症状草稿】\n").append(symptomDraft).append("\n\n");
        }

        if (turns != null && !turns.isEmpty()) {
            userContent.append("【患者与 AI 问诊对话】\n");
            for (ConsultationTurn turn : turns) {
                userContent.append("患者：").append(nullSafe(turn.getUserMessage())).append("\n");
                userContent.append("AI：").append(nullSafe(turn.getAssistantMessage())).append("\n");
            }
        }

        if (userContent.isEmpty()) {
            userContent.append("患者尚未提供任何信息，请生成空病历。");
        }

        userContent.append("\n请根据以上信息提取结构化预问诊病历。");
        messages.add(new UserMessage(userContent.toString()));

        Prompt prompt = new Prompt(messages);
        String response;
        try {
            response = chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM 调用生成结构化 EMR 失败，返回空病历: {}", e.getMessage(), e);
            return emptyEmr();
        }

        try {
            PreConsultationEmrDTO emr = converter.convert(response);
            return normalize(emr);
        } catch (Exception e) {
            log.warn("LLM 输出结构化 EMR 解析失败，尝试手动 JSON 解析: {}", e.getMessage());
            return parseManually(response);
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一位资深的预问诊医助。请根据患者症状草稿和问诊对话，提取以下 5 个字段的结构化预问诊病历。
                字段要求：
                1. chiefComplaint（主诉）：患者一句话概括当前最核心不适。
                2. presentIllness（现病史）：发病时间、症状部位、严重程度、持续时间、伴随症状、演变过程等。
                3. pastHistory（既往史）：患者自述的慢性病、手术史等；无则填"无"或"不详"。
                4. allergyHistory（过敏史）：药物/食物/其他过敏信息；无则填"无"或"不详"。
                5. suggestedExaminations（建议进一步检查项）：基于已有信息给出的检查建议，无则填"暂无"。

                请直接输出 JSON，不要包含任何解释性文字。
                """;
    }

    private PreConsultationEmrDTO parseManually(String response) {
        if (response == null || response.isBlank()) {
            return emptyEmr();
        }
        try {
            PreConsultationEmrDTO emr = objectMapper.readValue(response, PreConsultationEmrDTO.class);
            return normalize(emr);
        } catch (JsonProcessingException e) {
            log.error("手动解析结构化 EMR 失败: {}", e.getMessage());
            return emptyEmr();
        }
    }

    private PreConsultationEmrDTO normalize(PreConsultationEmrDTO emr) {
        if (emr == null) {
            return emptyEmr();
        }
        return new PreConsultationEmrDTO(
                nullToDefault(emr.chiefComplaint(), "暂无"),
                nullToDefault(emr.presentIllness(), "暂无"),
                nullToDefault(emr.pastHistory(), "不详"),
                nullToDefault(emr.allergyHistory(), "不详"),
                nullToDefault(emr.suggestedExaminations(), "暂无")
        );
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private PreConsultationEmrDTO emptyEmr() {
        return new PreConsultationEmrDTO("暂无", "暂无", "不详", "不详", "暂无");
    }

    private String nullSafe(String text) {
        return text == null ? "" : text;
    }
}
