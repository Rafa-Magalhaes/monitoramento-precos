package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.dto.request.LoginRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.LoginResponseDTO;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Value("${api.security.pepper:MinhaChavePepperSecreta123!}")
    private String pepper;

    public LoginResponseDTO autenticar(LoginRequestDTO dto) {
        String emailTratado = dto.getEmail().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(emailTratado)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        String senhaComPepper = dto.getSenha() + pepper;

        if (!passwordEncoder.matches(senhaComPepper, usuario.getSenha())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        String token = tokenService.gerarToken(usuario);

        return LoginResponseDTO.builder()
                .token(token)
                .build();
    }
}