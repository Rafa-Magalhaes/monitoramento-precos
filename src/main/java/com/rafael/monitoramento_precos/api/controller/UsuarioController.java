package com.rafael.monitoramento_precos.api.controller;

import com.rafael.monitoramento_precos.api.converter.UsuarioConverter;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateEmailRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.UsuarioUpdateTelefoneRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.UsuarioResponseDTO;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.domain.service.UsuarioService;
import com.rafael.monitoramento_precos.infrastructure.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioConverter usuarioConverter;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
            @Valid @RequestBody UsuarioCreateRequestDTO requestDTO) {

        Usuario usuarioSalvo = usuarioService.criarUsuario(requestDTO);
        UsuarioResponseDTO responseDTO = usuarioConverter.toResponseDTO(usuarioSalvo);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PatchMapping("/me/email")
    public ResponseEntity<Void> atualizarEmail(
            @Valid @RequestBody UsuarioUpdateEmailRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        usuarioService.atualizarEmail(jwtToken.getUsuarioId(), requestDTO);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/telefone")
    public ResponseEntity<Void> atualizarTelefone(
            @Valid @RequestBody UsuarioUpdateTelefoneRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        usuarioService.atualizarTelefone(jwtToken.getUsuarioId(), requestDTO);
        
        return ResponseEntity.noContent().build();
    }
}