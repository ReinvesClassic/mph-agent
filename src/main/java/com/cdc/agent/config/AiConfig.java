package com.cdc.agent.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cdc.agent.agent.Assistant;
import com.cdc.agent.store.JpaChatMemoryStore;
import com.cdc.agent.tool.CalculatorTool;
import com.cdc.agent.tool.WarehouseTool;
import com.cdc.agent.tool.WeatherTool;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

@Configuration
public class AiConfig {


    //获取Gemini的model
    @Bean
    public ChatModel geminiModel(@Value("${ai.gemini.api-key}") String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3.5-flash")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public Assistant assistant(
            ChatModel model,
            CalculatorTool calculatorTool,
            WeatherTool weatherTool,
            WarehouseTool warehouseTool,
            JpaChatMemoryStore jpaChatMemoryStore
    ){
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(calculatorTool,
                        weatherTool,
                        warehouseTool
                )
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .chatMemoryStore(jpaChatMemoryStore)
                                .maxMessages(20)
                                .build()
                )
                .build();
    }

}
