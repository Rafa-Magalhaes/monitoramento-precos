package com.rafael.monitoramento_precos.api.dto.response;

import com.rafael.monitoramento_precos.domain.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UsuarioResponseDTO {
    private UUID id;
    private String nome;
    private String email;
    private String telefone;
    private Role role;
}