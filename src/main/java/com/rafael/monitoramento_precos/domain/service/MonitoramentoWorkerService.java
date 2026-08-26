package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.HistoricoPreco;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.repository.MissaoBuscaRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.MercadoLivreScraperService;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoramentoWorkerService {

    private final MissaoBuscaRepository missaoBuscaRepository;
    private final MercadoLivreScraperService mercadoLivreScraperService;
    private final NotificacaoWhatsAppService notificacaoWhatsAppService;

    private record MissaoNaFila(MissaoBusca missao, int tentativaAtual) {}

    @Scheduled(cron = "0 0 3,15 * * *")
    public void executarMonitoramentoDiario() {
        log.info("Iniciando rotina de Web Scraping autônoma com Fila de Retry...");

        List<MissaoBusca> missoesAtivas = missaoBuscaRepository.findByAtivoTrue();

        if (missoesAtivas.isEmpty()) {
            log.info("Nenhuma missão ativa encontrada. Rotina finalizada.");
            return;
        }

        Queue<MissaoNaFila> fila = new LinkedList<>();
        missoesAtivas.forEach(missao -> fila.add(new MissaoNaFila(missao, 1)));

        int totalMissoes = missoesAtivas.size();
        int missoesZeradas = 0;

        while (!fila.isEmpty()) {
            MissaoNaFila itemAtual = fila.poll();
            MissaoBusca missao = itemAtual.missao();
            int tentativa = itemAtual.tentativaAtual();

            try {
                log.info("Processando missão: [{}] (Tentativa {}/3)", missao.getTermoDaBusca(), tentativa);
                List<ProdutoScrapedDTO> produtos = mercadoLivreScraperService.buscarProdutos(missao);

                if (produtos.isEmpty()) {
                    log.warn("Nenhum produto válido encontrado para a missão: {}", missao.getTermoDaBusca());
                    missoesZeradas++;
                } else {
                    processarESalvarHistorico(missao, produtos);
                }

                Thread.sleep(5000);

            } catch (Exception e) {
                log.error("Falha de rede/proxy na missão [{}]: {}", missao.getTermoDaBusca(), e.getMessage());

                if (tentativa < 3) {
                    log.warn("Enviando missão [{}] para o FINAL da fila de reprocessamento...", missao.getTermoDaBusca());
                    fila.add(new MissaoNaFila(missao, tentativa + 1));
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("🚨 Missão [{}] ABORTADA após 3 tentativas falhas. O Proxy não conseguiu resolver.", missao.getTermoDaBusca());
                }
            }
        }

        verificarHealthCheck(totalMissoes, missoesZeradas);
    }

    @Scheduled(cron = "0 0 0 * * *") // Roda todo dia exatamente à 00:00 (Meia-noite)
    public void desativarMissoesExpiradas() {
        log.info("Iniciando varredura de Missões Zumbis (Expiradas após 6 meses)...");

        List<MissaoBusca> missoesVencidas = missaoBuscaRepository
                .findByAtivoTrueAndDataExpiracaoBefore(LocalDateTime.now());

        if (missoesVencidas.isEmpty()) {
            log.info("Nenhuma missão expirada encontrada hoje.");
            return;
        }

        missoesVencidas.forEach(missao -> missao.setAtivo(false));

        missaoBuscaRepository.saveAll(missoesVencidas);

        log.info("Limpeza concluída! {} missões foram desativadas por expiração de prazo.", missoesVencidas.size());
    }

    private void processarESalvarHistorico(MissaoBusca missao, List<ProdutoScrapedDTO> produtos) {

        ProdutoScrapedDTO produtoMaisBarato = produtos.stream()
                .min(Comparator.comparing(ProdutoScrapedDTO::getPreco))
                .orElseThrow();

        BigDecimal precoMedio = produtos.stream()
                .map(ProdutoScrapedDTO::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(produtos.size()), 2, RoundingMode.HALF_UP);

        BigDecimal mediaAntiga = missao.getMediaPrecoHistorico();

        HistoricoPreco fotoDoDia = HistoricoPreco.builder()
                .precoMinimo(produtoMaisBarato.getPreco())
                .precoMedio(precoMedio)
                .linkProdutoMaisBarato(produtoMaisBarato.getLinkProduto())
                .dataCaptura(LocalDateTime.now())
                .build();

        missao.getHistoricoDePrecos().add(fotoDoDia);
        missao.setMediaPrecoHistorico(precoMedio);

        missaoBuscaRepository.save(missao);

        log.info("Histórico salvo para '{}' | Mínimo: R${} | Médio: R${}",
                missao.getTermoDaBusca(), produtoMaisBarato.getPreco(), precoMedio);

        notificacaoWhatsAppService.processarGatilhosENotificar(missao, produtoMaisBarato, precoMedio, mediaAntiga);
    }

    private void verificarHealthCheck(int totalMissoes, int missoesZeradas) {
        if (totalMissoes == 0) return;

        double taxaFalha = (double) missoesZeradas / totalMissoes;

        if (taxaFalha >= 0.30) {
            log.error("🚨 ALERTA CRÍTICO DE SISTEMA (HEALTH CHECK) 🚨");
            log.error("Taxa de falha atingiu {}% ({} de {} missões zeradas).",
                    String.format("%.1f", taxaFalha * 100), missoesZeradas, totalMissoes);

            String motivo = String.format("A taxa de falha atingiu %.1f%% (%d de %d missões zeradas). Possível Teste A/B no DOM.", taxaFalha * 100, missoesZeradas, totalMissoes);
            notificacaoWhatsAppService.notificarHealthCheckAdmin(motivo);
        }
    }
}