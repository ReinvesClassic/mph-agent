package com.cdc.agent;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * API 接口调用测试客户端
 * 演示如何使用 SM3 签名认证调用受保护的 API 接口
 *
 * 使用方式：
 * 1. 先启动 mph-agent 服务
 * 2. 运行本类的 main 方法
 *
 * 签名算法：
 *   签名原文 = method + path + timestamp + body
 *   sign = SM3(appSecret + 签名原文)
 *
 * 请求头：
 *   X-App-Id:    客户端应用ID
 *   X-Timestamp: 请求时间戳（毫秒）
 *   X-Sign:      SM3 签名值（Hex）
 */
public class ApiTestClient {

    // ==================== 配置区域 ====================

    /** 服务地址 */
    private static final String BASE_URL = "http://localhost:8888";

    /** 客户端 appId（从管理接口或配置文件获取） */
    private static final String APP_ID = "default";

    /** 客户端 appSecret（从管理接口或配置文件获取） */
    private static final String APP_SECRET = "default-secret-change-me";

    // ==================== 核心签名方法 ====================

    /**
     * 生成 SM3 签名
     *
     * @param method    HTTP 方法（GET/POST/PUT/DELETE）
     * @param path      请求路径（如 /mph/agent/chat）
     * @param timestamp 时间戳（毫秒）
     * @param body      请求体（GET 请求传空字符串）
     * @param appSecret 客户端密钥
     * @return SM3 签名值（Hex 编码）
     */
    public static String generateSign(String method, String path, String timestamp,
                                       String body, String appSecret) {
        // 构建签名原文: method + path + timestamp + body
        String signData = method + path + timestamp + (body != null ? body : "");
        // SM3 摘要: appSecret + signData
        String content = appSecret + signData;
        return SmUtil.sm3().digestHex(content);
    }

    // ==================== HTTP 请求方法 ====================

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送带 SM3 签名的 POST 请求
     */
    public static String post(String path, Object bodyObj) throws Exception {
        String body = objectMapper.writeValueAsString(bodyObj);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = generateSign("POST", path, timestamp, body, APP_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 发送带 SM3 签名的 GET 请求
     */
    public static String get(String path) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = generateSign("GET", path, timestamp, "", APP_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 发送带 SM3 签名的 PUT 请求
     */
    public static String put(String path, String queryString) throws Exception {
        String fullPath = path + (queryString != null ? "?" + queryString : "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = generateSign("PUT", fullPath, timestamp, "", APP_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + fullPath))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 发送带 SM3 签名的 DELETE 请求
     */
    public static String delete(String path, String queryString) throws Exception {
        String fullPath = path + (queryString != null ? "?" + queryString : "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = generateSign("DELETE", fullPath, timestamp, "", APP_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + fullPath))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // ==================== SM4 对称加密工具 ====================

    /**
     * SM4 加密（用于加密敏感请求体）
     *
     * @param data 明文 JSON
     * @param key  密钥（16 字节）
     * @return 密文（Hex 编码）
     */
    public static String sm4Encrypt(String data, String key) {
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "SM4");
        SM4 sm4 = new SM4(Mode.ECB, Padding.PKCS5Padding, secretKey);
        return sm4.encryptHex(data);
    }

    /**
     * SM4 解密（用于解密服务端响应）
     *
     * @param encryptedData 密文（Hex 编码）
     * @param key           密钥（16 字节）
     * @return 明文
     */
    public static String sm4Decrypt(String encryptedData, String key) {
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "SM4");
        SM4 sm4 = new SM4(Mode.ECB, Padding.PKCS5Padding, secretKey);
        return sm4.decryptStr(encryptedData);
    }

    // ==================== 测试用例 ====================

    public static void main(String[] args) {
        // 注册 Bouncy Castle 安全提供者（SM2/SM3/SM4 依赖）
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        System.out.println("========================================");
        System.out.println("  MPH Agent API 测试客户端");
        System.out.println("  SM3 签名认证 + SM4 加密");
        System.out.println("========================================\n");

        try {
            // ---------- 测试 1: 创建新客户端（无需认证） ----------
            System.out.println("【测试 1】创建客户端（无需认证）");
            System.out.println("----------------------------------------");
            // 管理接口无需签名，直接调用
            String createResult = postNoAuth("/mph/agent/auth/client",
                    Map.of("appName", "测试应用", "remark", "API测试"));
            System.out.println("响应: " + createResult);
            System.out.println();

            // ---------- 测试 2: 查询客户端列表（无需认证） ----------
            System.out.println("【测试 2】查询客户端列表（无需认证）");
            System.out.println("----------------------------------------");
            String listResult = getNoAuth("/mph/agent/auth/clients");
            System.out.println("响应: " + listResult);
            System.out.println();

            // ---------- 测试 3: 发送聊天消息（需 SM3 签名） ----------
            System.out.println("【测试 3】发送聊天消息（SM3 签名认证）");
            System.out.println("----------------------------------------");
            Map<String, String> chatBody = new HashMap<>();
            chatBody.put("userId", "user001");
            chatBody.put("conversationId", "");
            chatBody.put("message", "你好，请介绍一下你自己");
            String chatResult = post("/mph/agent/chat", chatBody);
            System.out.println("响应: " + chatResult);
            System.out.println();

            // ---------- 测试 4: 查询会话列表（需 SM3 签名） ----------
            System.out.println("【测试 4】查询会话列表（SM3 签名认证）");
            System.out.println("----------------------------------------");
            String convResult = get("/mph/agent/conversations?userId=user001");
            System.out.println("响应: " + convResult);
            System.out.println();

            // ---------- 测试 5: 知识库状态查询（需 SM3 签名） ----------
            System.out.println("【测试 5】知识库状态（SM3 签名认证）");
            System.out.println("----------------------------------------");
            String statusResult = get("/mph/agent/knowledge/status");
            System.out.println("响应: " + statusResult);
            System.out.println();

            // ---------- 测试 5.5: 目录摄入（首次初始化知识库） ----------
            System.out.println("【测试 5.5】目录摄入知识库（SM3 签名认证）");
            System.out.println("----------------------------------------");
            String ingestResult = post("/mph/agent/knowledge/ingest-dir?path=./knowledge-base", "");
            System.out.println("响应: " + ingestResult);
            System.out.println();

            // ---------- 测试 6: 知识库检索（需 SM3 签名） ----------
            System.out.println("【测试 6】知识库检索（SM3 签名认证）");
            System.out.println("----------------------------------------");
            String searchResult = get("/mph/agent/knowledge/search?query=仓库位置");
            System.out.println("响应: " + searchResult);
            System.out.println();

            // ---------- 测试 7: SM4 加密/解密演示 ----------
            System.out.println("【测试 7】SM4 对称加密/解密演示");
            System.out.println("----------------------------------------");
            String sm4Key = "1234567890abcdef"; // 16 字节密钥
            String plainText = "{\"userId\":\"user001\",\"message\":\"加密消息测试\"}";
            String encrypted = sm4Encrypt(plainText, sm4Key);
            String decrypted = sm4Decrypt(encrypted, sm4Key);
            System.out.println("明文: " + plainText);
            System.out.println("SM4 加密: " + encrypted);
            System.out.println("SM4 解密: " + decrypted);
            System.out.println("验证: " + plainText.equals(decrypted));
            System.out.println();

            // ---------- 测试 8: 签名验证失败演示 ----------
            System.out.println("【测试 8】签名验证失败演示（篡改请求体）");
            System.out.println("----------------------------------------");
            String tamperedTimestamp = String.valueOf(System.currentTimeMillis());
            String tamperedBody = "{\"userId\":\"hacker\",\"message\":\"非法请求\"}";
            // 使用错误的 appSecret 生成签名
            String wrongSign = generateSign("POST", "/mph/agent/chat",
                    tamperedTimestamp, tamperedBody, "wrong-secret");
            HttpRequest wrongRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/mph/agent/chat"))
                    .header("Content-Type", "application/json")
                    .header("X-App-Id", APP_ID)
                    .header("X-Timestamp", tamperedTimestamp)
                    .header("X-Sign", wrongSign)
                    .POST(HttpRequest.BodyPublishers.ofString(tamperedBody))
                    .build();
            HttpResponse<String> wrongResponse = httpClient.send(wrongRequest,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("响应（预期 401）: " + wrongResponse.body());
            System.out.println();

            System.out.println("========================================");
            System.out.println("  所有测试执行完毕");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("测试执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 无需认证的请求方法 ====================

    /**
     * 无需认证的 POST 请求（用于管理接口）
     */
    private static String postNoAuth(String path, Object bodyObj) throws Exception {
        String body = objectMapper.writeValueAsString(bodyObj);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * 无需认证的 GET 请求（用于管理接口）
     */
    private static String getNoAuth(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
