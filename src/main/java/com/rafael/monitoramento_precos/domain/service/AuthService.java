package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.dto.request.LoginRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.LoginResponseDTO;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${api.security.pepper:MinhaChavePepperSecreta123!}")
    private String pepper;

    private String hashFantasma;

    @PostConstruct
    public void inicializarHashFantasma() {
        this.hashFantasma = passwordEncoder.encode("senha-inexistente-123");
    }

    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        String emailTratado = dto.getEmail().toLowerCase().trim();
        String senhaComPepper = dto.getSenha() + pepper;

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailTratado);

        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(senhaComPepper, hashFantasma);
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(senhaComPepper, usuario.getSenha())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        String token = tokenService.gerarToken(usuario);

        return LoginResponseDTO.builder()
                .token(token)
                .build();
    }
}