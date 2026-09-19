package com.cdc.agent.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.cdc.agent.config.AuthProperties;
import com.cdc.agent.entity.AppClientEntity;
import com.cdc.agent.service.AppClientService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API 认证过滤器
 * 验证请求头中的 SM3 签名，防止未授权访问
 *
 * 请求头要求:
 *   X-App-Id:    客户端应用ID
 *   X-Timestamp: 请求时间戳（毫秒）
 *   X-Sign:      SM3 签名值（Hex）
 *
 * 签名算法: SM3(appSecret + method + path + timestamp + bodyHash)
 */
@Component
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private static final String HEADER_APP_ID = "X-App-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_SIGN = "X-Sign";

    private final AuthProperties authProperties;
    private final AppClientService appClientService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthFilter(AuthProperties authProperties,
                      AppClientService appClientService,
                      ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.appClientService = appClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 认证未启用，直接放行
        if (!authProperties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // 白名单路径放行
        String path = httpRequest.getRequestURI();
        for (String pattern : authProperties.getExcludePaths()) {
            if (pathMatcher.match(pattern, path)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 提取认证头
        String appId = httpRequest.getHeader(HEADER_APP_ID);
        String timestamp = httpRequest.getHeader(HEADER_TIMESTAMP);
        String sign = httpRequest.getHeader(HEADER_SIGN);

        if (appId == null || timestamp == null || sign == null) {
            writeError(httpResponse, 401, "缺少认证头（需要 X-App-Id, X-Timestamp, X-Sign）");
            return;
        }

        // 时间戳校验（防重放）
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            long tolerance = authProperties.getTimestampTolerance() * 1000;
            if (Math.abs(now - ts) > tolerance) {
                writeError(httpResponse, 401, "请求已过期，请检查客户端时间戳");
                return;
            }
        } catch (NumberFormatException e) {
            writeError(httpResponse, 401, "时间戳格式错误");
            return;
        }

        // 查找客户端
        AppClientEntity client = appClientService.findByAppId(appId);
        if (client == null) {
            writeError(httpResponse, 401, "无效的 appId");
            return;
        }

        // 客户端状态校验
        if (!client.getEnabled()) {
            writeError(httpResponse, 403, "客户端已被禁用");
            return;
        }

        // 读取请求体用于签名验证
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(httpRequest);
        String body = wrappedRequest.getCachedBody();

        // 构建签名原文: method + path + timestamp + body
        String method = httpRequest.getMethod();
        String signData = method + path + timestamp + body;

        // SM3 签名验证
        if (!appClientService.verifySign(signData, client.getAppSecret(), sign)) {
            writeError(httpResponse, 401, "签名验证失败");
            return;
        }

        log.debug("[Auth] 认证通过: appId={}, path={}", appId, path);
        chain.doFilter(wrappedRequest, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("code", status);
        error.put("message", message);
        error.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
