package com.cdc.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.cdc.agent.service.AppClientService;

import jakarta.annotation.PostConstruct;

/**
 * 认证模块配置
 * 启动时初始化默认客户端
 */
@Configuration
public class AuthConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    private final AuthProperties authProperties;
    private final AppClientService appClientService;

    public AuthConfig(AuthProperties authProperties, AppClientService appClientService) {
        this.authProperties = authProperties;
        this.appClientService = appClientService;
    }

    @PostConstruct
    public void init() {
        if (authProperties.isEnabled()) {
            AuthProperties.DefaultClient defaultClient = authProperties.getDefaultClient();
            appClientService.ensureDefaultClient(
                    defaultClient.getAppId(),
                    defaultClient.getAppSecret(),
                    defaultClient.getAppName()
            );
            log.info("[Auth] API 认证已启用，时间戳容忍窗口: {}s", authProperties.getTimestampTolerance());
        } else {
            log.warn("[Auth] API 认证已禁用，所有接口将无需认证即可访问！");
        }
    }
}
