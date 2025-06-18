package com.baber.identityservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI apiInfo() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Identity Service Server");

        return new OpenAPI()
                .info(new Info()
                        .title("Identity Service API")
                        .description("API Documentation for Identity Service")
                        .version("1.0.0"))
                .servers(List.of(server));
    }
}
