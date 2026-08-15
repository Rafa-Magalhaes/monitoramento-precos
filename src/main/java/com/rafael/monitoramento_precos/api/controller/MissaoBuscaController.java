package com.rafael.monitoramento_precos.api.controller;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.MissaoBuscaResponseDTO;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.service.MissaoBuscaService;
import com.rafael.monitoramento_precos.infrastructure.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/missoes")
@RequiredArgsConstructor
public class MissaoBuscaController {

    private final MissaoBuscaService missaoBuscaService;
    private final MissaoBuscaConverter missaoBuscaConverter;

    @PostMapping
    public ResponseEntity<MissaoBuscaResponseDTO> cadastrar(
            @Valid @RequestBody MissaoBuscaCreateRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        MissaoBusca missaoSalva = missaoBuscaService.criarMissao(requestDTO, jwtToken.getUsuarioId());
        MissaoBuscaResponseDTO responseDTO = missaoBuscaConverter.toResponseDTO(missaoSalva);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
}