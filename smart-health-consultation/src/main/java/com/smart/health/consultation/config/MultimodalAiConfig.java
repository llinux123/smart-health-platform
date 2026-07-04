package com.smart.health.consultation.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多模态 AI 配置：为图片分析创建独立的 ChatModel（智谱 AI GLM-4V）
 * 文本问诊仍使用百炼 qwen3.7-max（由 Spring AI 自动配置）
 */
@Configuration
public class MultimodalAiConfig {

    @Value("${spring.ai-zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    @Value("${spring.ai-zhipu.api-key:}")
    private String apiKey;

    @Value("${spring.ai-zhipu.model:glm-4v-flashx}")
    private String model;

    @Bean("multimodalChatModel")
    public OpenAiChatModel multimodalChatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath("/chat/completions")
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .build())
                .build();
    }
}
