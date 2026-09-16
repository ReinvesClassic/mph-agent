package com.cdc.agent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_memory_id", columnList = "memory_id"),
        @Index(name = "idx_conversation_id", columnList = "conversation_id")
})
@Data
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * 会话ID（LangChain4j 记忆存储键）
     */
    @Column(name = "memory_id", nullable = false)
    private String memoryId;

    /**
     * 会话ID（明确关联 conversation 表）
     */
    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;


    /**
     * USER / AI / SYSTEM
     */
    @Column(nullable = false)
    private String role;


    /**
     * 消息内容
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;


    /**
     * token数量
     */
    @Column(name = "token_count")
    private Integer tokenCount;


    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

