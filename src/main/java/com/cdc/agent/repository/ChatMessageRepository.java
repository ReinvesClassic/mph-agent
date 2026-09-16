package com.cdc.agent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cdc.agent.entity.ChatMessageEntity;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 查询某个会话的所有消息
     */
    List<ChatMessageEntity> findByMemoryIdOrderByCreateTimeAsc(String memoryId);

    /**
     * 根据 conversationId 查询某个会话的所有消息
     */
    List<ChatMessageEntity> findByConversationIdOrderByCreateTimeAsc(String conversationId);

    /**
     * 删除会话
     */
    @Transactional
    void deleteByMemoryId(String memoryId);

}

