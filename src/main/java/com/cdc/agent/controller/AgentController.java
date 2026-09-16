package com.cdc.agent.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cdc.agent.dto.ApiResponse;
import com.cdc.agent.dto.ChatRequest;
import com.cdc.agent.dto.ChatResponse;
import com.cdc.agent.dto.CreateConversationRequest;
import com.cdc.agent.entity.ConversationEntity;
import com.cdc.agent.exception.AgentException;
import com.cdc.agent.service.ChatService;
import com.cdc.agent.service.ConversationService;

@RestController
@RequestMapping("/mph/agent")
public class AgentController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @Value("${ai.system-prompt}")
    private String systemPrompt;

    public AgentController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    /**
     * 发送聊天消息
     *
     * @test curl -X POST http://localhost:8888/mph/agent/chat \
     *       -H "Content-Type: application/json" \
     *       -d '{"userId":"user001","conversationId":"","message":"你好"}'
     */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
        // 参数校验
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw AgentException.paramError("userId 不能为空");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw AgentException.paramError("message 不能为空");
        }
        System.out.println("-------------");

        // 获取有效会话（未提供或已过期则自动创建新会话）
        String conversationId = conversationService.getOrCreateActiveConversation(
                request.getUserId(), request.getConversationId());

        // 调用 AI（动态 System Prompt + 工具调用 + 记忆管理）
        String reply = chatService.chat(conversationId, systemPrompt, request.getMessage());

        // 首次对话后自动生成会话标题（异步执行，失败不影响聊天响应）
        conversationService.generateAndSetTitle(conversationId);

        return ApiResponse.ok(new ChatResponse(conversationId, reply));
    }

    /**
     * 创建新会话
     *
     * @test curl -X POST http://localhost:8888/mph/agent/conversation \
     *       -H "Content-Type: application/json" \
     *       -d '{"userId":"user001"}'
     */
    @PostMapping("/conversation")
    public ApiResponse<String> createConversation(@RequestBody CreateConversationRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw AgentException.paramError("userId 不能为空");
        }
        String conversationId = conversationService.createConversation(request.getUserId());
        return ApiResponse.ok(conversationId);
    }

    /**
     * 查询用户所有会话
     *
     * @test curl http://localhost:8888/mph/agent/conversations?userId=user001
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationEntity>> listConversations(@RequestParam String userId) {
        if (userId == null || userId.isBlank()) {
            throw AgentException.paramError("userId 不能为空");
        }
        List<ConversationEntity> conversations = conversationService.listConversations(userId);
        return ApiResponse.ok(conversations);
    }
}
