package com.cdc.agent.service.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档服务
 * 负责文档的摄入（加载 → 切分 → 向量化 → 存储）和检索
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Value("${rag.knowledge-base-dir:./knowledge-base}")
    private String knowledgeBaseDir;

    @Value("${rag.max-results:3}")
    private int maxResults;

    @Value("${rag.min-score:0.7}")
    private double minScore;

    @Value("${rag.chunk-size:300}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:50}")
    private int chunkOverlap;

    public DocumentService(EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 摄入单个上传文件
     * 将文件保存到知识库目录，然后加载、切分、向量化并存入向量库
     */
    public int ingestFile(MultipartFile file) throws IOException {
        Path dir = Paths.get(knowledgeBaseDir);
        Files.createDirectories(dir);

        // 保存文件到知识库目录
        Path targetPath = dir.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[RAG] 文件已保存: {}", targetPath);

        // 加载并摄入
        Document document = FileSystemDocumentLoader.loadDocument(targetPath);
        ingestDocuments(List.of(document));
        return 1;
    }

    /**
     * 从指定目录摄入所有文档
     */
    public int ingestDirectory(String dirPath) {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.warn("[RAG] 目录不存在: {}", dirPath);
            return 0;
        }

        Collection<Document> docs = FileSystemDocumentLoader.loadDocuments(dir);
        if (!docs.isEmpty()) {
            ingestDocuments(docs);
        }
        return docs.size();
    }

    /**
     * 检索与查询相关的知识库内容
     * Milvus 持久化存储，重启后数据仍在，无需检查 ingestedCount
     *
     * @param query 用户查询
     * @return 相关文档片段列表（按相似度排序）
     */
    public List<String> retrieveRelevantContext(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        List<String> context = result.matches().stream()
                .map(EmbeddingMatch::embedded)
                .map(TextSegment::text)
                .collect(Collectors.toList());

        log.info("[RAG] 检索到 {} 个相关片段 (query={})", context.size(), query);
        return context;
    }

    /**
     * 核心摄入流程：文档 → 切分 → 向量化 → 存储
     */
    private void ingestDocuments(Collection<Document> documents) {
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        for (Document doc : documents) {
            ingestor.ingest(doc);
            log.info("[RAG] 已摄入文档: {}", doc.metadata().getString("file_name"));
        }
    }
}
