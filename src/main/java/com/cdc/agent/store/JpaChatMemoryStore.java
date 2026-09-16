package com.cdc.agent.store;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.cdc.agent.entity.ChatMessageEntity;
import com.cdc.agent.repository.ChatMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final String CACHE_KEY_PREFIX = "chat:memory:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final ChatMessageRepository repository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        String cacheKey = CACHE_KEY_PREFIX + id;

        // 先查 Redis 缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                List<Map<String, String>> list = objectMapper.readValue(cached,
                        new TypeReference<List<Map<String, String>>>() {});
                return list.stream()
                        .map(this::convertMapToChatMessage)
                        .toList();
            } catch (Exception e) {
                // 反序列化失败，清除缓存，回源 MySQL
                stringRedisTemplate.delete(cacheKey);
            }
        }

        // 缓存未命中，查 MySQL
        List<ChatMessage> messages = repository
                .findByMemoryIdOrderByCreateTimeAsc(id)
                .stream()
                .map(this::convertToChatMessage)
                .toList();

        // 写入 Redis 缓存
        saveToRedis(cacheKey, messages);

        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        String cacheKey = CACHE_KEY_PREFIX + id;

        // 写 MySQL
        repository.deleteByMemoryId(id);
        List<ChatMessageEntity> entities = messages.stream()
                .map(message -> convertToEntity(id, message))
                .toList();
        repository.saveAll(entities);

        // 更新 Redis 缓存
        saveToRedis(cacheKey, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        String cacheKey = CACHE_KEY_PREFIX + id;

        // 删 MySQL
        repository.deleteByMemoryId(id);
        // 删 Redis 缓存
        stringRedisTemplate.delete(cacheKey);
    }

    /**
     * 将消息列表序列化后写入 Redis
     */
    private void saveToRedis(String cacheKey, List<ChatMessage> messages) {
        try {
            List<Map<String, String>> cacheData = messages.stream()
                    .map(this::convertToMap)
                    .toList();
            String json = objectMapper.writeValueAsString(cacheData);
            stringRedisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 序列化失败不影响主流程，只是不缓存
        }
    }

    /**
     * 将 ChatMessage 转换为 Map（用于 Redis 缓存）
     */
    private Map<String, String> convertToMap(ChatMessage message) {
        Map<String, String> map = new HashMap<>();
        if (message instanceof UserMessage um) {
            map.put("role", "USER");
            String text = um.contents().stream()
                    .filter(c -> c instanceof TextContent)
                    .map(c -> ((TextContent) c).text())
                    .findFirst()
                    .orElse("");
            map.put("content", text);
        } else if (message instanceof AiMessage ai) {
            map.put("role", "AI");
            map.put("content", ai.text());
        } else if (message instanceof SystemMessage sm) {
            map.put("role", "SYSTEM");
            map.put("content", sm.text());
        } else if (message instanceof ToolExecutionResultMessage term) {
            map.put("role", "TOOL_EXECUTION_RESULT");
            map.put("content", term.text());
        } else {
            map.put("role", "UNKNOWN");
            map.put("content", "");
        }
        return map;
    }

    /**
     * 将 Map 转换为 ChatMessage（从 Redis 缓存读取）
     */
    private ChatMessage convertMapToChatMessage(Map<String, String> map) {
        String role = map.get("role");
        String content = map.getOrDefault("content", "");
        return switch (role) {
            case "USER" -> UserMessage.from(content);
            case "AI" -> AiMessage.from(content);
            case "SYSTEM" -> SystemMessage.from(content);
            case "TOOL_EXECUTION_RESULT" ->
                    ToolExecutionResultMessage.from(null, null, content);
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
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
        entity.setConversationId(memoryId);
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