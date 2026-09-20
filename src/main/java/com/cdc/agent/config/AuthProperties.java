package com.cdc.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API 认证配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 是否启用 API 认证 */
    private boolean enabled = true;

    /** 签名时间戳容忍窗口（秒），默认 5 分钟 */
    private long timestampTolerance = 300;

    /** 默认客户端配置 */
    private DefaultClient defaultClient = new DefaultClient();

    /** 认证白名单路径（不需要认证的接口） */
    private String[] excludePaths = {
            "/mph/agent/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Data
    public static class DefaultClient {
        /** 默认 appId */
        private String appId = "default";
        /** 默认 appSecret */
        private String appSecret = "default-secret-change-me";
        /** 默认应用名称 */
        private String appName = "默认客户端";
    }
}
