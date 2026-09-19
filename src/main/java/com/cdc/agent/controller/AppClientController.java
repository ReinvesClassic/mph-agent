package com.cdc.agent.controller;

import com.cdc.agent.dto.ApiResponse;
import com.cdc.agent.dto.AppClientDTO;
import com.cdc.agent.dto.AppClientSecretDTO;
import com.cdc.agent.dto.CreateAppClientRequest;
import com.cdc.agent.exception.AgentException;
import com.cdc.agent.service.AppClientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 客户端管理接口
 * 提供客户端的增删改查、启用/禁用、密钥重置等功能
 * 该路径在 AuthFilter 白名单中，无需认证
 */
@RestController
@RequestMapping("/mph/agent/auth")
public class AppClientController {

    private final AppClientService appClientService;

    public AppClientController(AppClientService appClientService) {
        this.appClientService = appClientService;
    }

    /**
     * 创建客户端（返回密钥信息，仅此次可见）
     *
     * @test curl -X POST http://localhost:8888/mph/agent/auth/client \
     *       -H "Content-Type: application/json" \
     *       -d '{"appName":"测试应用","remark":"测试用"}'
     */
    @PostMapping("/client")
    public ApiResponse<AppClientSecretDTO> createClient(@RequestBody CreateAppClientRequest request) {
        if (request.getAppName() == null || request.getAppName().isBlank()) {
            throw AgentException.paramError("appName 不能为空");
        }
        AppClientSecretDTO secret = appClientService.createClient(
                request.getAppName(), request.getRemark());
        return ApiResponse.ok(secret);
    }

    /**
     * 查询所有客户端列表
     *
     * @test curl http://localhost:8888/mph/agent/auth/clients
     */
    @GetMapping("/clients")
    public ApiResponse<List<AppClientDTO>> listClients() {
        return ApiResponse.ok(appClientService.listClients());
    }

    /**
     * 查询单个客户端详情
     *
     * @test curl http://localhost:8888/mph/agent/auth/client?appId=xxx
     */
    @GetMapping("/client")
    public ApiResponse<AppClientDTO> getClient(@RequestParam String appId) {
        if (appId == null || appId.isBlank()) {
            throw AgentException.paramError("appId 不能为空");
        }
        return ApiResponse.ok(appClientService.getClient(appId));
    }

    /**
     * 启用/禁用客户端
     *
     * @test curl -X PUT "http://localhost:8888/mph/agent/auth/client/toggle?appId=xxx&enabled=false"
     */
    @PutMapping("/client/toggle")
    public ApiResponse<AppClientDTO> toggleEnabled(@RequestParam String appId,
                                                   @RequestParam boolean enabled) {
        if (appId == null || appId.isBlank()) {
            throw AgentException.paramError("appId 不能为空");
        }
        return ApiResponse.ok(appClientService.toggleEnabled(appId, enabled));
    }

    /**
     * 删除客户端
     *
     * @test curl -X DELETE "http://localhost:8888/mph/agent/auth/client?appId=xxx"
     */
    @DeleteMapping("/client")
    public ApiResponse<Void> deleteClient(@RequestParam String appId) {
        if (appId == null || appId.isBlank()) {
            throw AgentException.paramError("appId 不能为空");
        }
        appClientService.deleteClient(appId);
        return ApiResponse.ok();
    }

    /**
     * 重置客户端密钥（重新生成 appSecret 和 SM2 密钥对）
     *
     * @test curl -X POST "http://localhost:8888/mph/agent/auth/client/reset?appId=xxx"
     */
    @PostMapping("/client/reset")
    public ApiResponse<AppClientSecretDTO> resetSecret(@RequestParam String appId) {
        if (appId == null || appId.isBlank()) {
            throw AgentException.paramError("appId 不能为空");
        }
        return ApiResponse.ok(appClientService.resetSecret(appId));
    }
}
