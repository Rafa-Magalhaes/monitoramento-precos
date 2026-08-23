package com.rafael.monitoramento_precos.api.converter;

import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.UsuarioResponseDTO;
import com.rafael.monitoramento_precos.domain.enums.Role;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioConverter {

    public Usuario toEntity(UsuarioCreateRequestDTO dto, String senhaHash) {
        return Usuario.builder()
                .nome(dto.getNome().trim())
                .email(dto.getEmail().toLowerCase().trim())
                .senha(senhaHash)
                .telefone(dto.getTelefone())
                .role(Role.ROLE_USER)
                .ativo(true)
                .build();
    }

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .role(usuario.getRole())
                .build();
    }
}