package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.HistoricoPreco;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.repository.MissaoBuscaRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.KabumScraperService;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoramentoWorkerService {

    private final MissaoBuscaRepository missaoBuscaRepository;
    private final KabumScraperService kabumScraperService;

    @Scheduled(cron = "0 0 2,14 * * *")
    public void executarMonitoramentoDiario() {
        log.info("Iniciando rotina de Web Scraping autônoma...");

        List<MissaoBusca> missoesAtivas = missaoBuscaRepository.findByAtivoTrue();

        if (missoesAtivas.isEmpty()) {
            log.info("Nenhuma missão ativa encontrada. Rotina finalizada.");
            return;
        }

        int totalProdutosEncontradosNaRodada = 0;

        for (MissaoBusca missao : missoesAtivas) {
            try {
                List<ProdutoScrapedDTO> produtos = kabumScraperService.buscarProdutos(missao);

                if (produtos.isEmpty()) {
                    log.warn("Nenhum produto válido encontrado para a missão: {}", missao.getTermoDaBusca());
                    continue;
                }

                totalProdutosEncontradosNaRodada += produtos.size();
                processarESalvarHistorico(missao, produtos);

            } catch (Exception e) {
                // Se uma missão falhar, o try-catch garante que o robô não capote e continue para a próxima!
                log.error("Falha isolada ao processar missão ID: {}", missao.getId(), e);
            }
        }

        verificarHealthCheck(missoesAtivas.size(), totalProdutosEncontradosNaRodada);
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

        // TODO: Chamar o Domínio de Notificação (WhatsApp) aqui na próxima Vertical Slice!
    }

    private void verificarHealthCheck(int qtdMissoes, int totalProdutosEncontrados) {
        if (qtdMissoes > 0 && totalProdutosEncontrados == 0) {
            log.error("🚨 ALERTA CRÍTICO DE SISTEMA (HEALTH CHECK) 🚨");
            log.error("O Motor de Scraping retornou 0 resultados para TODAS as missões.");
            log.error("Isso indica um provável bloqueio de IP ou mudança drástica no CSS da Kabum.");
            // TODO: Integrar disparo direto de WhatsApp para o número do Administrador!
        }
    }
}