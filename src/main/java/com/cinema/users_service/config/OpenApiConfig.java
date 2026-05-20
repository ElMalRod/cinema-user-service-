package com.cinema.users_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI usersOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Cinema Users Service API")
                        .version("1.0")
                        .description("Microservicio de perfiles y wallets"));
    }
}
