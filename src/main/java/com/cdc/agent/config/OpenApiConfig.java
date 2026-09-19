package com.cdc.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 * 访问地址: http://localhost:8888/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MPH Agent API")
                        .version("1.0.0")
                        .description("AI 智能代理接口，支持国密 SM3 签名认证")
                        .contact(new Contact().name("CDC Team")))
                .components(new Components()
                        .addSecuritySchemes("AppAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-App-Id")
                                .description("客户端应用ID"))
                        .addSecuritySchemes("TimestampAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Timestamp")
                                .description("请求时间戳（毫秒）"))
                        .addSecuritySchemes("SignAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Sign")
                                .description("SM3 签名值（Hex）")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("AppAuth")
                        .addList("TimestampAuth")
                        .addList("SignAuth"));
    }
}
