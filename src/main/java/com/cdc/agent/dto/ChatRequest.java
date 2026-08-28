package com.cdc.agent.dto;

import lombok.Data;

@Data
public class ChatRequest {

    /**
     * 用户ID（必填）
     */
    private String userId;

    /**
     * 会话ID（可选，为空则自动创建新会话）
     */
    private String conversationId;

    /**
     * 消息内容（必填）
     */
    private String message;
}
