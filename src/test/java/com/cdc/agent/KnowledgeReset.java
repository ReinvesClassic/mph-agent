package com.cdc.agent;

import cn.hutool.crypto.SmUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;

/**
 * 重置知识库并验证检索
 */
public class KnowledgeReset {

    private static final String BASE_URL = "http://localhost:8888";
    private static final String APP_ID = "default";
    private static final String APP_SECRET = "default-secret-change-me";

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        HttpClient client = HttpClient.newHttpClient();

        // 1. 重置知识库（清除 + 重新摄入）
        System.out.println("=== 重置知识库 ===");
        String resetPath = "/mph/agent/knowledge/reset?path=./knowledge-base";
        String ts = String.valueOf(System.currentTimeMillis());
        String sign = SmUtil.sm3().digestHex(APP_SECRET + "POST" + resetPath + ts);
        
        HttpRequest resetReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + resetPath))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", ts)
                .header("X-Sign", sign)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        
        String resetResp = client.send(resetReq, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println("重置结果: " + resetResp);

        // 2. 等待索引刷新
        Thread.sleep(2000);

        // 3. 验证检索
        System.out.println("\n=== 验证检索 ===");
        String searchPath = "/mph/agent/knowledge/search?query=仓库位置";
        String ts2 = String.valueOf(System.currentTimeMillis());
        String sign2 = SmUtil.sm3().digestHex(APP_SECRET + "GET" + searchPath + ts2);
        
        HttpRequest searchReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + searchPath))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", ts2)
                .header("X-Sign", sign2)
                .GET()
                .build();
        
        String searchResp = client.send(searchReq, HttpResponse.BodyHandlers.ofString()).body();
        System.out.println("检索结果: " + searchResp);
    }
}
