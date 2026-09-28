package com.example.gateway.config;

import com.example.gateway.security.AccessTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/usuarios/cadastro",
                                "/api/auth/login",
                                "/api/auth/refresh").permitAll()
                        .pathMatchers(
                                HttpMethod.GET,
                                "/clientes",
                                "/clientes/**").authenticated()
                        .pathMatchers(
                                HttpMethod.POST,
                                "/pedidos",
                                "/pedidos/**").authenticated()
                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        byte[] chaveDecodificada =
                Base64.getDecoder().decode(jwtSecret);

        SecretKey secretKey = new SecretKeySpec(chaveDecodificada, "HmacSHA256");

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

        OAuth2TokenValidator<Jwt> validadorPadrao = JwtValidators.createDefault();

        OAuth2TokenValidator<Jwt> validadorCompleto =
                new DelegatingOAuth2TokenValidator<>(validadorPadrao, new AccessTokenValidator());

        decoder.setJwtValidator(validadorCompleto);

        return decoder;
    }
}