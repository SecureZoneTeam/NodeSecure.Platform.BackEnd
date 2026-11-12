package com.safezone.platform.shared.infrastructure.configuration.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI safeZoneOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SafeZone Platform API")
                        .description("NodeSecure Backend REST API documentation for Profiles, Subscriptions, and Reporting.")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://spring.io")));
    }
}
