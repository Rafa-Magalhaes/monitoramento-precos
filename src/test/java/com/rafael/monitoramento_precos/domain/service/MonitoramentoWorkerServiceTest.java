package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.repository.MissaoBuscaRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.MercadoLivreScraperService;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MonitoramentoWorkerServiceTest {

    @InjectMocks
    private MonitoramentoWorkerService workerService;

    @Mock
    private MissaoBuscaRepository missaoBuscaRepository;
    @Mock
    private MercadoLivreScraperService scraperService;
    @Mock
    private NotificacaoWhatsAppService notificacaoWhatsAppService;

    @Test
    void desativarMissoesExpiradas_DeveDesativarESalvarMissoesVencidas() {
        // Cenário: Criamos uma missão ativa que supostamente já expirou
        MissaoBusca missao = MissaoBusca.builder().ativo(true).build();

        // Simula que o banco encontrou essa missão quando buscamos as expiradas
        Mockito.when(missaoBuscaRepository.findByAtivoTrueAndDataExpiracaoBefore(Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of(missao));

        // Ação: Rodamos o nosso "Coveiro"
        workerService.desativarMissoesExpiradas();

        // Verificação: A flag 'ativo' deve ter virado false, e o saveAll deve ter sido chamado
        Assertions.assertFalse(missao.getAtivo());
        Mockito.verify(missaoBuscaRepository, Mockito.times(1)).saveAll(List.of(missao));
    }

    @Test
    void executarMonitoramentoDiario_DeveCalcularPrecosENotificarCorretamente() throws Exception {
        // Cenário: Uma missão ativa procurando por "RTX 4060"
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("RTX 4060").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));

        // Simula o extrator HTML devolvendo dois produtos com preços diferentes
        ProdutoScrapedDTO p1 = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link1").build();
        ProdutoScrapedDTO p2 = ProdutoScrapedDTO.builder().preco(new BigDecimal("200.00")).linkProduto("link2").build();

        Mockito.when(scraperService.buscarProdutos(missao)).thenReturn(List.of(p1, p2));

        // Ação: Dispara o robô
        workerService.executarMonitoramentoDiario();

        // Técnica Sênior: Capturador de Argumentos (ArgumentCaptor)
        // Como o método save() recebe a missão que foi alterada lá dentro, nós capturamos ela para inspecionar
        ArgumentCaptor<MissaoBusca> captor = ArgumentCaptor.forClass(MissaoBusca.class);
        Mockito.verify(missaoBuscaRepository).save(captor.capture());
        MissaoBusca missaoSalva = captor.getValue();

        // Verificação Matemática: A média de 100 e 200 tem que ser exatos 150.00
        Assertions.assertEquals(new BigDecimal("150.00"), missaoSalva.getMediaPrecoHistorico());
        // O produto mais barato salvo no histórico tem que ser o de 100.00
        Assertions.assertEquals(new BigDecimal("100.00"), missaoSalva.getHistoricoDePrecos().get(0).getPrecoMinimo());

        // Verifica se o WhatsApp foi chamado com a média e o produto correto,
        // e aceita nulo para a média antiga (pois a missão era nova no teste)
        Mockito.verify(notificacaoWhatsAppService)
                .processarGatilhosENotificar(Mockito.eq(missao), Mockito.eq(p1), Mockito.eq(new BigDecimal("150.00")), Mockito.nullable(BigDecimal.class));
    }

    @Test
    void executarMonitoramentoDiario_DeveAcionarHealthCheck_QuandoZeroResultados() throws Exception {
        // Cenário: A missão existe, mas o scraper devolve uma lista vazia (simulando bloqueio do Mercado Livre)
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("RTX 4060").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));
        Mockito.when(scraperService.buscarProdutos(missao)).thenReturn(List.of());

        // Ação
        workerService.executarMonitoramentoDiario();

        // Verificação: O alarme de emergência DEVE ter sido disparado
        Mockito.verify(notificacaoWhatsAppService).notificarHealthCheckAdmin();
    }
}