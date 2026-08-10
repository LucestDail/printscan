package com.printscan.cloud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 문서 메타 — /swagger-ui.html · /v3/api-docs.
 * cloud 인증은 헤더 3종: 디바이스=X-Device-Token, SaaS 관리=X-Admin-Token, 테넌트=X-Org-Key(또는 세션).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI cloudOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("printscan cloud (hub) API")
                        .version("v2")
                        .description("허브 — 멀티테넌트 플릿 관리·집계·원격 인쇄 지시. "
                                + "디바이스 아웃바운드(X-Device-Token), 관리 API(세션/X-Admin-Token), 테넌트(X-Org-Key)."))
                .components(new Components()
                        .addSecuritySchemes("deviceToken", apiKey("X-Device-Token"))
                        .addSecuritySchemes("adminToken", apiKey("X-Admin-Token"))
                        .addSecuritySchemes("orgKey", apiKey("X-Org-Key")));
    }

    private SecurityScheme apiKey(String header) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER).name(header);
    }
}
