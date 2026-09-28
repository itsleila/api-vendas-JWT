package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator rotas(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service-auth", rota -> rota.path("/api/auth/**").uri("lb://AUTH-SERVICE"))
                .route("auth-service-usuarios", rota -> rota.path("/usuarios/**").uri("lb://AUTH-SERVICE"))
                .route("clientes-service", rota -> rota.path("/clientes/**").uri("lb://CLIENTES-SERVICE"))
                .route("vendas-service", rota -> rota.path("/pedidos/**", "/vendas/**").uri("lb://VENDAS-SERVICE"))
                .route("produtos-service", rota -> rota.path("/produtos/**").uri("lb://PRODUTOS-SERVICE"))
                .build();
    }
}