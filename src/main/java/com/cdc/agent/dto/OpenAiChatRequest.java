package com.cdc.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completion 请求格式
 * 兼容 OpenAI API: POST /v1/chat/completions
 */
@Data
public class OpenAiChatRequest {

    /**
     * 模型名称（可选，服务端使用默认模型）
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数（0-2），默认 1.0
     */
    private Double temperature;

    /**
     * 是否流式响应
     */
    private Boolean stream;

    @Data
    public static class Message {
        /**
         * 角色: system / user / assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }
}
