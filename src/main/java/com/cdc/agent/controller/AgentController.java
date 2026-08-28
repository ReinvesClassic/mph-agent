package com.cdc.agent.controller;

import com.cdc.agent.agent.Assistant;
import com.cdc.agent.dto.ApiResponse;
import com.cdc.agent.dto.ChatRequest;
import com.cdc.agent.dto.ChatResponse;
import com.cdc.agent.dto.CreateConversationRequest;
import com.cdc.agent.entity.ConversationEntity;
import com.cdc.agent.exception.AgentException;
import com.cdc.agent.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mph/agent")
public class AgentController {

    private final Assistant assistant;
    private final ConversationService conversationService;

    public AgentController(Assistant assistant, ConversationService conversationService) {
        this.assistant = assistant;
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

        // 获取有效会话（未提供或已过期则自动创建新会话）
        String conversationId = conversationService.getOrCreateActiveConversation(
                request.getUserId(), request.getConversationId());

        // 调用 AI
        String reply = assistant.chat(conversationId, request.getMessage());

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
