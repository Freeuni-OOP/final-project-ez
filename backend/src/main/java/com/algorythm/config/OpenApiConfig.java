package com.algorythm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API docs metadata (springdoc-openapi generates the actual endpoint list from
 * the controllers/DTOs themselves, so this only supplies the title/description
 * and the "Authorize" button's JWT bearer scheme for trying protected routes
 * directly from Swagger UI at /swagger-ui/index.html).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AlgoRythm API")
                        .description("Compositions, publishing, likes, comments, follows, and the feeds "
                                + "built on top of them. Auto-generated from the backend's controllers - "
                                + "always reflects what's actually deployed.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}