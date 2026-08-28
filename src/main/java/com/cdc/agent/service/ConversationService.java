package com.cdc.agent.service;

import com.cdc.agent.entity.ConversationEntity;
import com.cdc.agent.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    /**
     * 会话过期时间（分钟）：30 分钟不活跃则视为过期
     */
    private static final int EXPIRE_MINUTES = 30;

    private final ConversationRepository conversationRepository;

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @return conversationId (UUID)
     */
    public String createConversation(String userId) {
        String conversationId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        ConversationEntity entity = new ConversationEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(userId);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setLastActiveTime(now);

        conversationRepository.save(entity);
        return conversationId;
    }

    /**
     * 根据 conversationId 查询会话
     */
    public ConversationEntity getConversation(String conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + conversationId));
    }

    /**
     * 查询用户所有会话
     */
    public List<ConversationEntity> listConversations(String userId) {
        return conversationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 判断会话是否已过期
     *
     * @param conversationId 会话ID
     * @return true 表示已过期或不存在
     */
    public boolean isExpired(String conversationId) {
        return conversationRepository.findByConversationId(conversationId)
                .map(entity -> {
                    if (entity.getLastActiveTime() == null) {
                        return true;
                    }
                    return entity.getLastActiveTime()
                            .plusMinutes(EXPIRE_MINUTES)
                            .isBefore(LocalDateTime.now());
                })
                .orElse(true); // 不存在也视为过期
    }

    /**
     * 刷新会话的最后活跃时间
     */
    @Transactional
    public void refreshActiveTime(String conversationId) {
        conversationRepository.findByConversationId(conversationId)
                .ifPresent(entity -> {
                    entity.setLastActiveTime(LocalDateTime.now());
                    entity.setUpdateTime(LocalDateTime.now());
                    conversationRepository.save(entity);
                });
    }

    /**
     * 获取或创建有效会话
     * 如果 conversationId 为空或已过期，自动创建新会话
     *
     * @param userId         用户ID
     * @param conversationId 会话ID（可为 null）
     * @return 有效的 conversationId
     */
    public String getOrCreateActiveConversation(String userId, String conversationId) {
        // 未提供 conversationId，创建新会话
        if (conversationId == null || conversationId.isBlank()) {
            return createConversation(userId);
        }

        // 已过期，创建新会话
        if (isExpired(conversationId)) {
            return createConversation(userId);
        }

        // 有效会话，刷新活跃时间
        refreshActiveTime(conversationId);
        return conversationId;
    }
}
