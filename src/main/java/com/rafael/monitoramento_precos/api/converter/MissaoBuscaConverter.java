package com.rafael.monitoramento_precos.api.converter;

import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.MissaoBuscaResponseDTO;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class MissaoBuscaConverter {

    public MissaoBusca toEntity(MissaoBuscaCreateRequestDTO dto, UUID usuarioId) {
        return MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca(dto.getTermoDaBusca())
                .precoAlvo(dto.getPrecoAlvo())
                .palavrasChaveExigidas(extrairPalavrasChave(dto.getTermoDaBusca()))
                .palavrasChaveProibidas(dto.getPalavrasChaveProibidas() != null ? dto.getPalavrasChaveProibidas() : new ArrayList<>())
                .build();
    }

    public MissaoBuscaResponseDTO toResponseDTO(MissaoBusca entity) {
        return MissaoBuscaResponseDTO.builder()
                .id(entity.getId())
                .termoDaBusca(entity.getTermoDaBusca())
                .precoAlvo(entity.getPrecoAlvo())
                .palavrasChaveExigidas(entity.getPalavrasChaveExigidas())
                .palavrasChaveProibidas(entity.getPalavrasChaveProibidas())
                .ativo(entity.getAtivo())
                .dataCriacao(entity.getDataCriacao())
                .build();
    }

    private List<String> extrairPalavrasChave(String termo) {
        if (termo == null || termo.isBlank()) {
            return new ArrayList<>();
        }
        // Remove espaços extras, converte para maiúsculo e transforma em uma lista
        return Arrays.stream(termo.trim().toUpperCase().split("\\s+"))
                .toList();
    }
}