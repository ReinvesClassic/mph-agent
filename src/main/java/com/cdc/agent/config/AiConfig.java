package com.cdc.agent.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Configuration
public class AiConfig {

    /**
     * Gemini 模型（默认）
     * 当 ai.model.provider=gemini 或未配置时启用
     */
    @Bean
    @ConditionalOnProperty(name = "ai.model.provider", havingValue = "gemini", matchIfMissing = true)
    public ChatModel geminiModel(
            @Value("${ai.gemini.api-key}") String apiKey,
            @Value("${ai.gemini.model-name:gemini-3.5-flash}") String modelName
    ) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Ollama 模型（通过 OpenAI 兼容接口调用）
     * 当 ai.model.provider=ollama 时启用
     */
    @Bean
    @ConditionalOnProperty(name = "ai.model.provider", havingValue = "ollama")
    public ChatModel ollamaModel(
            @Value("${ai.ollama.base-url:http://192.168.1.4:11434/v1}") String baseUrl,
            @Value("${ai.ollama.model-name:qwen3:8b}") String modelName,
            @Value("${ai.ollama.api-key:ollama}") String apiKey,
            @Value("${ai.ollama.timeout:60}") int timeout
    ) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .temperature(0.0)
                .timeout(Duration.ofSeconds(timeout))
                .build();
    }

}
