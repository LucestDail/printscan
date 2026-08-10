package com.printscan.edge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 문서 메타 — /swagger-ui.html · /v3/api-docs. edge 는 HTTP Basic 인증. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI edgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("printscan edge API")
                        .version("v2")
                        .description("온디바이스(라즈베리파이) — 라벨 렌더/인쇄(^GFA)·스캔·재고·허브 동기화. HTTP Basic 인증."))
                .addSecurityItem(new SecurityRequirement().addList("basic"))
                .components(new Components().addSecuritySchemes("basic",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }
}
