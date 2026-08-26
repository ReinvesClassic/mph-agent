package com.cdc.agent.store;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cdc.agent.entity.ChatMessageEntity;
import com.cdc.agent.repository.ChatMessageRepository;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaChatMemoryStore implements ChatMemoryStore {

    private final ChatMessageRepository repository;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        return repository
                .findByMemoryIdOrderByCreateTimeAsc(id)
                .stream()
                .map(this::convertToChatMessage)
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        // 先删除旧消息
        repository.deleteByMemoryId(id);
        // 保存最新消息
        List<ChatMessageEntity> entities = messages.stream()
                .map(message -> convertToEntity(id, message))
                .toList();
        repository.saveAll(entities);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        repository.deleteByMemoryId(memoryId.toString());
    }

    /**
     * 将数据库实体转换为 LangChain4j 的 ChatMessage
     * 根据 role 字段判断消息类型，还原为对应的子类
     */
    private ChatMessage convertToChatMessage(ChatMessageEntity entity) {
        return switch (entity.getRole()) {
            case "USER" -> UserMessage.from(entity.getContent());
            case "AI" -> AiMessage.from(entity.getContent());
            case "SYSTEM" -> SystemMessage.from(entity.getContent());
            case "TOOL_EXECUTION_RESULT" ->
                    ToolExecutionResultMessage.from(null, null, entity.getContent());
            default -> throw new IllegalArgumentException("Unknown role: " + entity.getRole());
        };
    }

    /**
     * 将 LangChain4j 的 ChatMessage 转换为数据库实体
     * 根据消息类型设置 role 字段，并提取文本内容
     */
    private ChatMessageEntity convertToEntity(String memoryId, ChatMessage message) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setMemoryId(memoryId);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        if (message instanceof UserMessage um) {
            entity.setRole("USER");
            // UserMessage 没有 text() 方法，需要从 contents() 中提取文本
            String text = um.contents().stream()
                    .filter(c -> c instanceof TextContent)
                    .map(c -> ((TextContent) c).text())
                    .findFirst()
                    .orElse("");
            entity.setContent(text);
        } else if (message instanceof AiMessage ai) {
            entity.setRole("AI");
            entity.setContent(ai.text());
        } else if (message instanceof SystemMessage sm) {
            entity.setRole("SYSTEM");
            entity.setContent(sm.text());
        } else if (message instanceof ToolExecutionResultMessage term) {
            entity.setRole("TOOL_EXECUTION_RESULT");
            entity.setContent(term.text());
        } else {
            entity.setRole("UNKNOWN");
        }

        return entity;
    }
}