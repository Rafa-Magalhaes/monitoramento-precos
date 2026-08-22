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

    // NOVO ENDPOINT: Buscar missão específica
    @GetMapping("/{id}")
    public ResponseEntity<MissaoBuscaResponseDTO> buscarPorId(
            @PathVariable String id,
            JwtAuthenticationToken jwtToken) {

        MissaoBusca missao = missaoBuscaService.buscarPorId(id, jwtToken.getUsuarioId());
        MissaoBuscaResponseDTO responseDTO = missaoBuscaConverter.toResponseDTO(missao);

        return ResponseEntity.ok(responseDTO);
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

    // REFATORAÇÃO: Agora retorna 204 No Content
    @PatchMapping("/{id}/termo")
    public ResponseEntity<Void> atualizarTermo(
            @PathVariable String id,
            @Valid @RequestBody MissaoBuscaUpdateTermoRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        missaoBuscaService.atualizarTermoBusca(id, jwtToken.getUsuarioId(), requestDTO);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/blacklist")
    public ResponseEntity<Void> atualizarBlacklist(
            @PathVariable String id,
            @RequestBody MissaoBuscaUpdateBlacklistRequestDTO requestDTO,
            JwtAuthenticationToken jwtToken) {

        missaoBuscaService.atualizarBlacklist(id, jwtToken.getUsuarioId(), requestDTO);

        return ResponseEntity.noContent().build();
    }
}