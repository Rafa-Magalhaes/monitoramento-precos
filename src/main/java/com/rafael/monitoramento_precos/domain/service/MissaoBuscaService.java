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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MissaoBuscaService {

    private final MissaoBuscaRepository missaoBuscaRepository;
    private final MissaoBuscaConverter missaoBuscaConverter;

    public MissaoBusca criarMissao(MissaoBuscaCreateRequestDTO dto, UUID usuarioId) {

        List<MissaoBusca> missoesDoUsuario = missaoBuscaRepository.findByUsuarioId(usuarioId);

        List<String> palavrasNovaMissao = missaoBuscaConverter.extrairPalavrasChave(dto.getTermoDaBusca());

        Set<String> setNovasPalavras = new HashSet<>(palavrasNovaMissao);

        boolean jaMonitoraEsseTermo = missoesDoUsuario.stream()
                .anyMatch(missao -> {
                    Set<String> setPalavrasExistentes = new HashSet<>(missao.getPalavrasChaveExigidas());
                    return setPalavrasExistentes.equals(setNovasPalavras) && missao.getAtivo();
                });

        if (jaMonitoraEsseTermo) {
            throw new ConflictException("Você já possui uma missão de busca ativa para estes mesmos termos.");
        }

        MissaoBusca novaMissao = missaoBuscaConverter.toEntity(dto, usuarioId);
        return missaoBuscaRepository.save(novaMissao);
    }

    public List<MissaoBusca> listarMissoesUsuario(UUID usuarioId) {
        return missaoBuscaRepository.findByUsuarioId(usuarioId);
    }

    // NOVO METODO: Busca uma missão específica garantindo que pertence ao usuário logado
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

        List<String> palavrasNovoTermo = missaoBuscaConverter.extrairPalavrasChave(dto.getTermoDaBusca());
        Set<String> setNovasPalavras = new HashSet<>(palavrasNovoTermo);

        boolean jaMonitoraEsseTermo = missaoBuscaRepository.findByUsuarioId(usuarioIdToken).stream()
                .anyMatch(m -> {
                    // Ignora a própria missão sendo editada e missões inativas
                    if (m.getId().equals(missaoId) || !m.getAtivo()) {
                        return false;
                    }
                    Set<String> setPalavrasExistentes = new HashSet<>(m.getPalavrasChaveExigidas());
                    return setPalavrasExistentes.equals(setNovasPalavras);
                });

        if (jaMonitoraEsseTermo) {
            throw new ConflictException("Você já possui outra missão de busca ativa para estes mesmos termos.");
        }

        missao.setTermoDaBusca(dto.getTermoDaBusca());

        missao.setPalavrasChaveExigidas(palavrasNovoTermo);

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