package com.cdc.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Chat Completion 响应格式
 * 兼容 OpenAI API 响应结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private Message message;
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
