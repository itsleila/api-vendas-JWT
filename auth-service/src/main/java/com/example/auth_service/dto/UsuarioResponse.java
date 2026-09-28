package com.example.auth_service.dto;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        boolean ativo
) {
}
