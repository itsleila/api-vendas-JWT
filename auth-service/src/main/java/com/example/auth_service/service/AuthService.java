package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RefreshTokenRequest;
import com.example.auth_service.security.JwtService;
import com.example.auth_service.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.senha()));
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo");
        }

        UserDetails usuario = usuarioDetailsService.loadUserByUsername(request.email().trim().toLowerCase());

        return gerarResposta(usuario);
    }

    public AuthResponse renovarToken(RefreshTokenRequest request) {
        try {
            String refreshToken = request.refreshToken();

            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O token informado não é um refresh token");
            }

            String email = jwtService.extrairEmail(refreshToken);

            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

            if (!jwtService.tokenValido(refreshToken, usuario)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado");
            }

            return gerarResposta(usuario);

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado");
        }
    }

    private AuthResponse gerarResposta(UserDetails usuario) {
        String accessToken = jwtService.gerarAccessToken(usuario);
        String refreshToken = jwtService.gerarRefreshToken(usuario);

        return new AuthResponse(
                "Bearer",
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration()
        );
    }
}
