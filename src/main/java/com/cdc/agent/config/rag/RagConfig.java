package com.cdc.agent.config.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置
 * 提供本地 Embedding 模型和 Milvus 持久化向量存储
 */
@Configuration
public class RagConfig {

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private int milvusPort;

    @Value("${milvus.collection-name:mph_knowledge}")
    private String collectionName;

    @Value("${milvus.dimension:384}")
    private int dimension;

    /**
     * 本地 Embedding 模型（AllMiniLmL6V2 量化版，~32MB）
     * 首次使用时自动下载模型文件，之后离线运行
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    /**
     * Milvus 持久化向量存储
     * 数据持久化到 Milvus，重启后不丢失
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(collectionName)
                .dimension(dimension)
                .build();
    }
}
