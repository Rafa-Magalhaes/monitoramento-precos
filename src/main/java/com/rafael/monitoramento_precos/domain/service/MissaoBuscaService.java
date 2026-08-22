package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.api.converter.MissaoBuscaConverter;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaUpdateBlacklistRequestDTO;
import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaUpdateTermoRequestDTO;
import com.rafael.monitoramento_precos.domain.exception.ConflictException;
import com.rafael.monitoramento_precos.domain.exception.ResourceNotFoundException;
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

    // NOVO MÉTODO: Busca uma missão específica garantindo que pertence ao usuário logado
    public MissaoBusca buscarPorId(String missaoId, UUID usuarioIdToken) {
        MissaoBusca missao = missaoBuscaRepository.findById(missaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Missão de busca não encontrada."));

        if (!missao.getUsuarioId().equals(usuarioIdToken)) {
            throw new ConflictException("Acesso negado. Você não tem permissão para acessar esta missão.");
        }

        return missao;
    }

    public void excluirMissao(String missaoId, UUID usuarioIdToken) {
        MissaoBusca missao = missaoBuscaRepository.findById(missaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Missão de busca não encontrada."));

        if (!missao.getUsuarioId().equals(usuarioIdToken)) {
            throw new ConflictException("Acesso negado. Você não tem permissão para excluir esta missão.");
        }

        missaoBuscaRepository.delete(missao);
    }

    public void excluirTodasMissoes(UUID usuarioIdToken) {
        missaoBuscaRepository.deleteByUsuarioId(usuarioIdToken);
    }

    public void atualizarTermoBusca(String missaoId, UUID usuarioIdToken, MissaoBuscaUpdateTermoRequestDTO dto) {
        MissaoBusca missao = missaoBuscaRepository.findById(missaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Missão de busca não encontrada."));

        if (!missao.getUsuarioId().equals(usuarioIdToken)) {
            throw new ConflictException("Acesso negado. Você não tem permissão para alterar esta missão.");
        }

        boolean jaMonitoraEsseTermo = missaoBuscaRepository.findByUsuarioId(usuarioIdToken).stream()
                .anyMatch(m -> !m.getId().equals(missaoId)
                        && m.getTermoDaBusca().equalsIgnoreCase(dto.getTermoDaBusca())
                        && m.getAtivo());

        if (jaMonitoraEsseTermo) {
            throw new ConflictException("Você já possui outra missão de busca ativa para este exato termo.");
        }

        missao.setTermoDaBusca(dto.getTermoDaBusca());
        missao.setPalavrasChaveExigidas(missaoBuscaConverter.extrairPalavrasChave(dto.getTermoDaBusca()));

        missaoBuscaRepository.save(missao);
    }

    public void atualizarBlacklist(String missaoId, UUID usuarioIdToken, MissaoBuscaUpdateBlacklistRequestDTO dto) {
        MissaoBusca missao = missaoBuscaRepository.findById(missaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Missão de busca não encontrada."));

        if (!missao.getUsuarioId().equals(usuarioIdToken)) {
            throw new ConflictException("Acesso negado. Você não tem permissão para alterar esta missão.");
        }

        List<String> novaBlacklist = dto.getPalavrasChaveProibidas() != null ? dto.getPalavrasChaveProibidas() : List.of();

        missao.setPalavrasChaveProibidas(novaBlacklist);

        missaoBuscaRepository.save(missao);
    }
}