package com.rafael.monitoramento_precos.api.controller;

import com.rafael.monitoramento_precos.api.dto.request.LoginRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.LoginResponseDTO;
import com.rafael.monitoramento_precos.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {

        LoginResponseDTO response = authService.autenticar(requestDTO);

        return ResponseEntity.ok(response);
    }
}