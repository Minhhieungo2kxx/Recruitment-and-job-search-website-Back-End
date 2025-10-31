package com.webjob.application.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
public class OpenAPIConfig {
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("Job Portal API - IT Job Platform")
                .version("1.0.0")
                .description("This API provides access to job listings, applications, company profiles, and candidate management for an online IT job platform.")
                .termsOfService("https://itviec.vn/terms")
                .contact(new Contact()
                        .name("JobPortal Dev Team")
                        .url("https://itviec.com")
                        .email("support@jobportal.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://choosealicense.com/licenses/mit/"));
    }


    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8081")
                        .description("Development server"),
                new Server()
                        .url("https://itviec.com")
                        .description("Production server")
        );
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
