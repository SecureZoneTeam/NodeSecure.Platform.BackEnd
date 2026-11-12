package com.safezone.nodesecure.platform.shared.infrastructure.configuration.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nodeSecurePlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NodeSecure Platform API")
                        .version("1.0.0")
                        .description("REST API documentation for the NodeSecure Platform (SafeZone) bounded contexts: "
                                + "Inventory (products and stock movements) and Alerts (security alerts).")
                        .contact(new Contact().name("SafeZone Team"))
                        .license(new License().name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
