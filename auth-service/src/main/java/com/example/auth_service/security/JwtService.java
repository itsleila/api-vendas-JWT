package com.example.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String gerarAccessToken(UserDetails usuario) {
        return gerarToken(usuario.getUsername(),
                ACCESS,
                accessTokenExpiration
        );
    }

    public String gerarRefreshToken(UserDetails usuario) {
        return gerarToken(usuario.getUsername(),
                REFRESH,
                refreshTokenExpiration
        );
    }

    private String gerarToken(String email, String tipo, long duracao
    ) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + duracao);

        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE, tipo)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(getSigningKey())
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean isAccessToken(String token) {
        return ACCESS.equals(
                extrairClaims(token).get(TOKEN_TYPE, String.class)
        );
    }

    public boolean isRefreshToken(String token) {
        return REFRESH.equals(
                extrairClaims(token).get(TOKEN_TYPE, String.class)
        );
    }

    public boolean tokenValido(
            String token,
            UserDetails usuario
    ) {
        String email = extrairEmail(token);

        return email.equalsIgnoreCase(usuario.getUsername()) && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return extrairClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
