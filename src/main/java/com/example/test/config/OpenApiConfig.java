package com.example.test.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme; // <-- Crucial: Must be the 'models' package
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet API")
                        .version("v1")
                        .description("API documentation for the Wallet service.")
                        .contact(new Contact().name("Jide")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP) // Now this resolves perfectly
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token in the format: Bearer <token>")));
    }
}