package com.cdc.agent.controller;

import com.cdc.agent.dto.ApiResponse;
import com.cdc.agent.exception.AgentException;
import com.cdc.agent.service.rag.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理接口
 * 提供文档上传、目录摄入、状态查询和测试检索功能
 */
@RestController
@RequestMapping("/mph/agent/knowledge")
@Tag(name = "知识库管理", description = "文档上传、摄入、检索接口（需 SM3 签名认证）")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传文档到知识库
     *
     * @test curl -X POST http://localhost:8888/mph/agent/knowledge/upload \
     *       -F "file=@/path/to/document.txt"
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档到知识库并进行向量化摄入")
    public ApiResponse<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw AgentException.paramError("文件不能为空");
        }
        try {
            documentService.ingestFile(file);
            return ApiResponse.ok("文档上传并摄入成功: " + file.getOriginalFilename());
        } catch (IOException e) {
            throw AgentException.paramError("文档摄入失败: " + e.getMessage());
        }
    }

    /**
     * 从指定目录批量摄入文档
     *
     * @test curl -X POST "http://localhost:8888/mph/agent/knowledge/ingest-dir?path=./knowledge-base"
     */
    @PostMapping("/ingest-dir")
    @Operation(summary = "目录批量摄入", description = "从指定目录批量摄入文档")
    public ApiResponse<String> ingestFromDirectory(@RequestParam String path) {
        if (path == null || path.isBlank()) {
            throw AgentException.paramError("目录路径不能为空");
        }
        int count = documentService.ingestDirectory(path);
        return ApiResponse.ok("从目录摄入 " + count + " 个文档: " + path);
    }

    /**
     * 查询知识库状态
     *
     * @test curl http://localhost:8888/mph/agent/knowledge/status
     */
    @GetMapping("/status")
    @Operation(summary = "知识库状态", description = "查询知识库存储状态")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("storageType", "milvus");
        status.put("status", "connected");
        return ApiResponse.ok(status);
    }

    /**
     * 测试检索（调试用）
     *
     * @test curl "http://localhost:8888/mph/agent/knowledge/search?query=仓库位置"
     */
    @GetMapping("/search")
    @Operation(summary = "测试检索", description = "根据查询内容检索相关知识库片段")
    public ApiResponse<List<String>> search(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            throw AgentException.paramError("查询内容不能为空");
        }
        List<String> results = documentService.retrieveRelevantContext(query);
        return ApiResponse.ok(results);
    }
}
