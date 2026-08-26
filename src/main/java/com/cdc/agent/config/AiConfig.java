package com.cdc.agent.config;

import com.cdc.agent.agent.Assistant;
import com.cdc.agent.tool.CalculatorTool;
import com.cdc.agent.tool.WarehouseTool;
import com.cdc.agent.tool.WeatherTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {


    //获取Gemini的model
    @Bean
    public ChatModel geminiModel(@Value("${ai.gemini.api-key}") String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.0)
                .build();
    }

    @Bean
    public Assistant assistant(
            ChatModel model,
            CalculatorTool calculatorTool,
            WeatherTool weatherTool,
            WarehouseTool warehouseTool

    ){
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(calculatorTool,
                        weatherTool,
                        warehouseTool
                )
                .build();
    }

}
