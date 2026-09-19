package com.cdc.agent.service;

import com.cdc.agent.store.JpaChatMemoryStore;
import com.cdc.agent.service.rag.DocumentService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务
 * 直接管理 ChatMemory 和工具执行，支持动态 System Prompt
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_TOOL_ROUNDS = 5;

    private final ChatModel chatModel;
    private final JpaChatMemoryStore memoryStore;
    private final DocumentService documentService;
    private final List<Object> toolObjects;
    private final List<ToolSpecification> toolSpecs;

    public ChatService(ChatModel chatModel,
                       JpaChatMemoryStore memoryStore,
                       DocumentService documentService,
                       com.cdc.agent.tool.CalculatorTool calculatorTool,
                       com.cdc.agent.tool.WeatherTool weatherTool,
                       com.cdc.agent.tool.WarehouseTool warehouseTool) {
        this.chatModel = chatModel;
        this.memoryStore = memoryStore;
        this.documentService = documentService;
        this.toolObjects = List.of(calculatorTool, weatherTool, warehouseTool);
        this.toolSpecs = new ArrayList<>();
        for (Object tool : this.toolObjects) {
            toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(tool));
        }
    }

    /**
     * 执行聊天
     *
     * @param conversationId 会话ID
     * @param systemPrompt   系统提示词（从配置注入）
     * @param userMessage    用户消息
     * @return AI 回复文本
     */
    public String chat(String conversationId, String systemPrompt, String userMessage) {
        // 加载记忆
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(memoryStore)
                .maxMessages(20)
                .build();

        List<ChatMessage> history = memory.messages();
        log.info("[记忆加载] conversationId={}, 历史消息数={}", conversationId, history.size());
        for (ChatMessage msg : history) {
            log.info("  - {}: {}", msg.type(), msg instanceof AiMessage ? ((AiMessage) msg).text() : "...");
        }

        // RAG: 从知识库检索相关上下文
        List<String> ragContext = documentService.retrieveRelevantContext(userMessage);
        String effectivePrompt = systemPrompt;
        if (!ragContext.isEmpty()) {
            String contextBlock = String.join("\n---\n", ragContext);
            effectivePrompt = systemPrompt + "\n\n以下是从知识库中检索到的相关信息，请优先基于这些信息回答用户问题：\n" + contextBlock;
            log.info("[RAG] 注入 {} 个知识库片段到系统提示", ragContext.size());
        }

        // 构建消息：系统提示 + 历史记忆 + 当前用户消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(effectivePrompt));
        messages.addAll(history);
        messages.add(UserMessage.from(userMessage));

        // 工具调用循环
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(toolSpecs)
                    .build();

            AiMessage aiReply = chatModel.chat(request).aiMessage();
            messages.add(aiReply);

            if (!aiReply.hasToolExecutionRequests()) {
                // 无工具调用，保存记忆并返回
                memory.add(UserMessage.from(userMessage));
                memory.add(aiReply);
                log.info("[记忆保存] conversationId={}, 保存后消息数={}", conversationId, memory.messages().size());
                return aiReply.text();
            }

            // 执行工具
            for (ToolExecutionRequest toolReq : aiReply.toolExecutionRequests()) {
                String result = executeTool(toolReq, conversationId);
                messages.add(ToolExecutionResultMessage.from(
                        toolReq.id(), toolReq.name(), result));
            }
        }

        return "工具调用次数超出限制";
    }

    /**
     * 执行单个工具调用
     */
    private String executeTool(ToolExecutionRequest request, String memoryId) {
        String toolName = request.name();
        log.info("执行工具: {}", toolName);

        for (Object toolObj : toolObjects) {
            for (Method method : toolObj.getClass().getMethods()) {
                if (method.isAnnotationPresent(Tool.class)
                        && method.getName().equals(toolName)) {
                    DefaultToolExecutor executor = new DefaultToolExecutor(toolObj, request);
                    return executor.execute(request, memoryId);
                }
            }
        }
        return "工具不存在: " + toolName;
    }
}
