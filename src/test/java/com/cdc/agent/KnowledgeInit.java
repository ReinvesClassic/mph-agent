package com.cdc.agent;

import cn.hutool.crypto.SmUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;

/**
 * 知识库初始化摄入工具
 * 将 knowledge-base 目录的文档摄入到 Milvus
 */
public class KnowledgeInit {

    private static final String BASE_URL = "http://localhost:8888";
    private static final String APP_ID = "default";
    private static final String APP_SECRET = "default-secret-change-me";

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        String path = "/mph/agent/knowledge/ingest-dir?path=./knowledge-base";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = "";
        String signData = "POST" + path + timestamp + body;
        String sign = SmUtil.sm3().digestHex(APP_SECRET + signData);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("X-App-Id", APP_ID)
                .header("X-Timestamp", timestamp)
                .header("X-Sign", sign)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("摄入结果: " + response.body());

        // 验证检索
        Thread.sleep(1000);
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

        HttpResponse<String> searchResp = HttpClient.newHttpClient()
                .send(searchReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("检索结果: " + searchResp.body());
    }
}
