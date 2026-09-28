package com.example.gateway.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERRO =
            new OAuth2Error("invalid_token", "É necessário informar um access token válido", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String tokenType = jwt.getClaimAsString("token_type");

        if ("access".equals(tokenType)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(ERRO);
    }
}