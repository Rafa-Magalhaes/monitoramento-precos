package com.rafael.monitoramento_precos.api.converter;

import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.response.MissaoBuscaResponseDTO;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDateTime;
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
                .dataExpiracao(LocalDateTime.now().plusMonths(6))
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

        // 1. Remove acentos e sinais gráficos
        String termoSemAcento = Normalizer.normalize(termo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // 2. Troca qualquer pontuação (vírgula, ponto, traço) por espaço em branco
        String termoLimpo = termoSemAcento.replaceAll("\\p{Punct}", " ");

        // 3. Converte para maiúsculo, recorta e FILTRA as Stop Words (1 ou 2 caracteres)
        return Arrays.stream(termoLimpo.trim().toUpperCase().split("\\s+"))
                .filter(palavra -> palavra.length() > 2) // Nova trava de segurança de negócio!
                .toList();
    }
}