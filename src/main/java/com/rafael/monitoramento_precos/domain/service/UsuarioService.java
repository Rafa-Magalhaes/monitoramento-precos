package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.UsuarioConverter;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateEmailRequestDTO;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.exception.ResourceNotFoundException;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioConverter usuarioConverter;

    @Value("${api.security.pepper:MinhaChavePepperSecreta123!}")
    private String pepper;

    @Transactional
    public Usuario criarUsuario(UsuarioCreateRequestDTO dto) {

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail.");
        }

        String senhaComPepper = dto.getSenha() + pepper;
        String senhaHash = passwordEncoder.encode(senhaComPepper);

        Usuario novoUsuario = usuarioConverter.toEntity(dto, senhaHash);

        return usuarioRepository.save(novoUsuario);
    }

    public void atualizarEmail(UUID usuarioIdToken, UsuarioUpdateEmailRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioIdToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (usuario.getEmail().equalsIgnoreCase(dto.getEmail())) {
            return;
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Este e-mail já está sendo utilizado por outra conta.");
        }

        usuario.setEmail(dto.getEmail());
        usuarioRepository.save(usuario);
    }
}