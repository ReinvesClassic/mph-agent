package com.cdc.agent.repository;

import com.cdc.agent.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    /**
     * 根据 conversationId 查询会话
     */
    Optional<ConversationEntity> findByConversationId(String conversationId);

    /**
     * 根据 userId 查询所有会话（按创建时间倒序）
     */
    List<ConversationEntity> findByUserIdOrderByCreateTimeDesc(String userId);
}
