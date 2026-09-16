package com.cdc.agent.controller;

import com.cdc.agent.dto.OpenAiChatRequest;
import com.cdc.agent.dto.OpenAiChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OpenAI 兼容 API 控制器
 * 提供 /v1/chat/completions 接口，兼容 OpenAI API 格式
 * 任何支持 OpenAI 格式的客户端（如 Open WebUI、ChatGPT-Next-Web 等）均可直接对接
 */
@RestController
@RequestMapping("/v1")
public class OpenAiCompatController {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatController.class);

    private final ChatModel chatModel;

    @Value("${ai.system-prompt}")
    private String systemPrompt;

    public OpenAiCompatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * OpenAI 兼容的 Chat Completion 接口
     *
     * @test curl -X POST http://localhost:8888/v1/chat/completions \
     *       -H "Content-Type: application/json" \
     *       -d '{"model":"qwen3:8b","messages":[{"role":"user","content":"你好"}]}'
     */
    @PostMapping("/chat/completions")
    public OpenAiChatResponse chatCompletions(@RequestBody OpenAiChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        String modelName = request.getModel() != null ? request.getModel() : "default";
        log.info("OpenAI 兼容接口收到请求, model={}, messages={}", modelName, request.getMessages().size());

        // 构建消息列表：系统提示 + 用户传入的消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        for (OpenAiChatRequest.Message msg : request.getMessages()) {
            String role = msg.getRole();
            String content = msg.getContent();
            if (content == null) content = "";

            switch (role) {
                case "system" -> messages.add(SystemMessage.from(content));
                case "user" -> messages.add(UserMessage.from(content));
                case "assistant" -> messages.add(AiMessage.from(content));
                default -> log.warn("未知角色: {}, 跳过", role);
            }
        }

        // 调用模型
        ChatResponse response = chatModel.chat(messages);
        String reply = response.aiMessage().text();

        // 构建 OpenAI 格式响应
        return OpenAiChatResponse.builder()
                .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24))
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(modelName)
                .choices(List.of(
                        OpenAiChatResponse.Choice.builder()
                                .index(0)
                                .message(OpenAiChatResponse.Message.builder()
                                        .role("assistant")
                                        .content(reply)
                                        .build())
                                .finishReason("stop")
                                .build()
                ))
                .build();
    }
}
