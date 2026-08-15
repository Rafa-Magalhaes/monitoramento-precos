package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.repository.MissaoBuscaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissaoBuscaService {

    private final MissaoBuscaRepository missaoBuscaRepository;
    private final MissaoBuscaConverter missaoBuscaConverter;

    public MissaoBusca criarMissao(MissaoBuscaCreateRequestDTO dto, UUID usuarioId) {

        List<MissaoBusca> missoesDoUsuario = missaoBuscaRepository.findByUsuarioId(usuarioId);

        boolean jaMonitoraEsseTermo = missoesDoUsuario.stream()
                .anyMatch(missao -> missao.getTermoDaBusca().equalsIgnoreCase(dto.getTermoDaBusca())
                        && missao.getAtivo());

        if (jaMonitoraEsseTermo) {
            throw new ConflictException("Você já possui uma missão de busca ativa para este exato termo.");
        }

        MissaoBusca novaMissao = missaoBuscaConverter.toEntity(dto, usuarioId);

        return missaoBuscaRepository.save(novaMissao);
    }

    public List<MissaoBusca> listarMissoesUsuario(UUID usuarioId) {
        return missaoBuscaRepository.findByUsuarioId(usuarioId);
    }
}