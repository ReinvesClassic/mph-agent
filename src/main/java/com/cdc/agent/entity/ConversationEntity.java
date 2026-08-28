package com.cdc.agent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "uk_conversation_id", columnList = "conversation_id", unique = true)
})
@Data
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话唯一ID (UUID)
     */
    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    /**
     * 会话标题
     */
    @Column(length = 256)
    private String title;

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

    /**
     * 最后活跃时间（用于判断会话是否过期）
     */
    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;
}
