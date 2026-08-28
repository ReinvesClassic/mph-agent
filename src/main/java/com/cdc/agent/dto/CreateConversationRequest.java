package com.cdc.agent.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {

    /**
     * 用户ID（必填）
     */
    private String userId;
}
