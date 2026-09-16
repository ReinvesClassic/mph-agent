package com.cdc.agent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cdc.agent.entity.ChatMessageEntity;
import com.cdc.agent.entity.ConversationEntity;
import com.cdc.agent.repository.ChatMessageRepository;
import com.cdc.agent.repository.ConversationRepository;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /**
     * 会话过期时间（分钟）：30 分钟不活跃则视为过期
     */
    private static final int EXPIRE_MINUTES = 30;

    /**
     * 标题最大长度
     */
    private static final int MAX_TITLE_LENGTH = 50;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatModel chatModel;

    public ConversationService(ConversationRepository conversationRepository,
                               ChatMessageRepository chatMessageRepository,
                               ChatModel chatModel) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatModel = chatModel;
    }

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

    /**
     * 根据第一条用户消息自动生成会话标题
     * 使用 AI 模型生成简短摘要作为标题
     *
     * @param conversationId 会话ID
     */
    public void generateAndSetTitle(String conversationId) {
        try {
            // 获取第一条用户消息
            List<ChatMessageEntity> messages = chatMessageRepository
                    .findByMemoryIdOrderByCreateTimeAsc(conversationId);
            String firstUserMessage = messages.stream()
                    .filter(m -> "USER".equals(m.getRole()))
                    .map(ChatMessageEntity::getContent)
                    .findFirst()
                    .orElse(null);

            if (firstUserMessage == null || firstUserMessage.isBlank()) {
                return;
            }

            // 调用 AI 生成简短标题
            List<dev.langchain4j.data.message.ChatMessage> msgs = List.of(
                    SystemMessage.from("根据用户的消息生成一个简短的会话标题，不超过10个字，只返回标题文本，不要加引号或其他符号。"),
                    UserMessage.from(firstUserMessage)
            );
            String rawTitle = chatModel.chat(msgs).aiMessage().text();

            // 截断过长标题
            final String title = (rawTitle != null && rawTitle.length() > MAX_TITLE_LENGTH)
                    ? rawTitle.substring(0, MAX_TITLE_LENGTH)
                    : rawTitle;

            // 更新会话标题
            conversationRepository.findByConversationId(conversationId)
                    .ifPresent(entity -> {
                        entity.setTitle(title);
                        entity.setUpdateTime(LocalDateTime.now());
                        conversationRepository.save(entity);
                    });

            log.info("会话 [{}] 标题已生成: {}", conversationId, title);
        } catch (Exception e) {
            log.warn("生成会话标题失败，不影响聊天功能: {}", e.getMessage());
        }
    }
}
