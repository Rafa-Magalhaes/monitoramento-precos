package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.UsuarioConverter;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateEmailRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateTelefoneRequestDTO;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.exception.ResourceNotFoundException;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final MissaoBuscaService missaoBuscaService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioConverter usuarioConverter;
    private final NotificacaoWhatsAppService notificacaoWhatsAppService;

    @Value("${api.security.pepper:MinhaChavePepperSecreta123!}")
    private String pepper;

    @Transactional
    public Usuario criarUsuario(UsuarioCreateRequestDTO dto) {

        String emailTratado = dto.getEmail().toLowerCase().trim();

        if (usuarioRepository.findByEmail(emailTratado).isPresent()) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail.");
        }

        String senhaComPepper = dto.getSenha() + pepper;
        String senhaHash = passwordEncoder.encode(senhaComPepper);

        Usuario novoUsuario = usuarioConverter.toEntity(dto, senhaHash);

        Usuario usuarioSalvo = usuarioRepository.save(novoUsuario);

        notificacaoWhatsAppService.enviarBoasVindas(usuarioSalvo);

        return usuarioSalvo;
    }

    public void atualizarEmail(UUID usuarioIdToken, UsuarioUpdateEmailRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioIdToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        String emailTratado = dto.getEmail().toLowerCase().trim();

        if (usuario.getEmail().equals(emailTratado)) {
            return;
        }

        if (usuarioRepository.existsByEmail(emailTratado)) {
            throw new ConflictException("Este e-mail já está sendo utilizado por outra conta.");
        }

        usuario.setEmail(emailTratado);
        usuarioRepository.save(usuario);
    }

    public void atualizarTelefone(UUID usuarioIdToken, UsuarioUpdateTelefoneRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioIdToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (usuario.getTelefone().equals(dto.getTelefone())) {
            return;
        }

        if (usuarioRepository.existsByTelefone(dto.getTelefone())) {
            throw new ConflictException("Este número de WhatsApp já está vinculado a outra conta.");
        }

        usuario.setTelefone(dto.getTelefone());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluirConta(UUID usuarioIdToken) {
        Usuario usuario = usuarioRepository.findById(usuarioIdToken)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        missaoBuscaService.excluirTodasMissoes(usuarioIdToken);

        usuarioRepository.delete(usuario);
    }
}