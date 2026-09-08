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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MissaoBuscaConverter {

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "do", "da", "dos", "das", "e", "o", "a", "os", "as",
            "em", "no", "na", "um", "uma", "para", "com", "por"
    );

    public MissaoBusca toEntity(MissaoBuscaCreateRequestDTO dto, UUID usuarioId) {
        List<String> palavrasExtraidas = extrairPalavrasChave(dto.getTermoDaBusca());

        String assinatura = palavrasExtraidas.stream()
                .distinct()
                .sorted()
                .collect(Collectors.joining("-"));

        return MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca(dto.getTermoDaBusca())
                .assinaturaBusca(assinatura)
                .precoAlvo(dto.getPrecoAlvo())
                .palavrasChaveExigidas(palavrasExtraidas)
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

    public List<String> extrairPalavrasChave(String termo) {
        if (termo == null || termo.isBlank()) {
            return new ArrayList<>();
        }

        // 1. Remove acentos e sinais gráficos
        String termoSemAcento = Normalizer.normalize(termo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        // 2. Troca qualquer pontuação (vírgula, ponto, traço) por espaço em branco
        String termoLimpo = termoSemAcento.replaceAll("\\p{Punct}", " ");

        // 3. Converte para maiúsculo, recorta e FILTRA as Stop Words baseando-se na lista explícita
        return Arrays.stream(termoLimpo.trim().toUpperCase().split("\\s+"))
                .filter(palavra -> !palavra.isBlank())

                .filter(palavra -> !STOP_WORDS.contains(palavra.toLowerCase()))
                .toList();
    }
}