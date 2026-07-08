package com.smart.health.consultation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.health.consultation.dto.PreConsultationEmrDTO;
import com.smart.health.consultation.entity.ConsultationTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * PreConsultationEmrGenerator — 预问诊电子病历生成单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PreConsultationEmrGenerator — 预问诊电子病历生成")
class PreConsultationEmrGeneratorTest {

    @Mock
    private OpenAiChatModel chatModel;

    private ObjectMapper objectMapper;

    private PreConsultationEmrGenerator emrGenerator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emrGenerator = new PreConsultationEmrGenerator(chatModel, objectMapper);
    }

    @Test
    @DisplayName("LLM 返回合法 JSON 时正确解析 5 个字段")
    void generate_validJson_returnsParsedEmr() {
        // Arrange
        String validJson = """
                {
                  "chiefComplaint": "头痛三天",
                  "presentIllness": "持续性胀痛，伴恶心，无呕吐",
                  "pastHistory": "无特殊",
                  "allergyHistory": "青霉素过敏",
                  "suggestedExaminations": "头颅CT、血常规"
                }
                """;
        mockLlmResponse(validJson);
        List<ConsultationTurn> turns = createTurns("我头痛", "请问头痛多久了？");

        // Act
        PreConsultationEmrDTO emr = emrGenerator.generate(turns, "头痛");

        // Assert
        assertThat(emr.chiefComplaint()).isEqualTo("头痛三天");
        assertThat(emr.presentIllness()).isEqualTo("持续性胀痛，伴恶心，无呕吐");
        assertThat(emr.pastHistory()).isEqualTo("无特殊");
        assertThat(emr.allergyHistory()).isEqualTo("青霉素过敏");
        assertThat(emr.suggestedExaminations()).isEqualTo("头颅CT、血常规");
    }

    @Test
    @DisplayName("LLM 返回非法 JSON 时降级为手动解析并返回空病历")
    void generate_invalidJson_fallsBackToManualParsing() {
        // Arrange
        String invalidJson = "这根本不是 JSON";
        mockLlmResponse(invalidJson);

        // Act
        PreConsultationEmrDTO emr = emrGenerator.generate(Collections.emptyList(), null);

        // Assert
        assertThat(emr.chiefComplaint()).isEqualTo("暂无");
        assertThat(emr.presentIllness()).isEqualTo("暂无");
        assertThat(emr.pastHistory()).isEqualTo("不详");
        assertThat(emr.allergyHistory()).isEqualTo("不详");
        assertThat(emr.suggestedExaminations()).isEqualTo("暂无");
    }

    @Test
    @DisplayName("对话为空且症状草稿为空时返回空病历")
    void generate_emptyConversationAndDraft_returnsEmptyEmr() {
        // Arrange
        mockLlmResponse("{}");

        // Act
        PreConsultationEmrDTO emr = emrGenerator.generate(Collections.emptyList(), null);

        // Assert
        assertThat(emr.chiefComplaint()).isEqualTo("暂无");
        assertThat(emr.presentIllness()).isEqualTo("暂无");
        assertThat(emr.pastHistory()).isEqualTo("不详");
        assertThat(emr.allergyHistory()).isEqualTo("不详");
        assertThat(emr.suggestedExaminations()).isEqualTo("暂无");
    }

    private void mockLlmResponse(String content) {
        AssistantMessage assistantMessage = new AssistantMessage(content);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    private List<ConsultationTurn> createTurns(String userMessage, String assistantMessage) {
        ConsultationTurn turn = new ConsultationTurn();
        turn.setUserMessage(userMessage);
        turn.setAssistantMessage(assistantMessage);
        return List.of(turn);
    }
}
