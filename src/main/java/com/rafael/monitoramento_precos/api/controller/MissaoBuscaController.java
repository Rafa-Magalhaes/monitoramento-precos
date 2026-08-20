package com.rafael.monitoramento_precos.api.controller;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaUpdateBlacklistRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaUpdateTermoRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.MissaoBuscaResponseDTO;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.service.MissaoBuscaService;
import com.rafael.monitoramento_precos.infrastructure.security.JwtAuthenticationToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<MissaoBuscaResponseDTO>> listar(JwtAuthenticationToken jwtToken) {

        List<MissaoBusca> missoes = missaoBuscaService.listarMissoesUsuario(jwtToken.getUsuarioId());

        List<MissaoBuscaResponseDTO> response = missoes.stream()
                .map(missaoBuscaConverter::toResponseDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable String id,
            JwtAuthenticationToken jwtToken) {

        missaoBuscaService.excluirMissao(id, jwtToken.getUsuarioId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> excluirTodas(JwtAuthenticationToken jwtToken) {

        missaoBuscaService.excluirTodasMissoes(jwtToken.getUsuarioId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/termo")
    public ResponseEntity<MissaoBuscaResponseDTO> atualizarTermo(
            @PathVariable String id,
            @Valid @RequestBody MissaoBuscaUpdateTermoRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        MissaoBusca missaoAtualizada = missaoBuscaService.atualizarTermoBusca(id, jwtToken.getUsuarioId(), requestDTO);
        MissaoBuscaResponseDTO responseDTO = missaoBuscaConverter.toResponseDTO(missaoAtualizada);

        return ResponseEntity.ok(responseDTO);
    }

    @PatchMapping("/{id}/blacklist")
    public ResponseEntity<MissaoBuscaResponseDTO> atualizarBlacklist(
            @PathVariable String id,
            @RequestBody MissaoBuscaUpdateBlacklistRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        MissaoBusca missaoAtualizada = missaoBuscaService.atualizarBlacklist(id, jwtToken.getUsuarioId(), requestDTO);
        MissaoBuscaResponseDTO responseDTO = missaoBuscaConverter.toResponseDTO(missaoAtualizada);

        return ResponseEntity.ok(responseDTO);
    }
}