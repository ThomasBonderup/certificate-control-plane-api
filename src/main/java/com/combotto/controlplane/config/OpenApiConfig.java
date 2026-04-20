package com.combotto.controlplane.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI controlPlaneOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                                .info(new Info()
                                                .title("Combotto Control Plane API")
                                                .version("v1")
                                                .description("Control plane API for certificate inventory, asset relationships, workflow state, and operational views.")
                                                .contact(new Contact().name("Thomas Bonderup")))
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .schemaRequirement(securitySchemeName,
                                                new SecurityScheme()
                                                                .name(securitySchemeName)
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT"));
        }
}
