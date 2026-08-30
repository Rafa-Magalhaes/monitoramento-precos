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
        MissaoBusca missao = MissaoBusca.builder().ativo(true).build();

        Mockito.when(missaoBuscaRepository.findByAtivoTrueAndDataExpiracaoBefore(Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of(missao));

        workerService.desativarMissoesExpiradas();

        Assertions.assertFalse(missao.getAtivo());
        Mockito.verify(missaoBuscaRepository, Mockito.times(1)).saveAll(List.of(missao));
    }

    @Test
    void executarMonitoramentoDiario_DeveCalcularPrecosENotificarCorretamente() throws Exception {
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("RTX 4060").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));

        ProdutoScrapedDTO p1 = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link1").build();
        ProdutoScrapedDTO p2 = ProdutoScrapedDTO.builder().preco(new BigDecimal("200.00")).linkProduto("link2").build();

        Mockito.when(scraperService.buscarProdutos(missao)).thenReturn(List.of(p1, p2));

        workerService.executarMonitoramentoDiario();

        ArgumentCaptor<MissaoBusca> captor = ArgumentCaptor.forClass(MissaoBusca.class);
        Mockito.verify(missaoBuscaRepository).save(captor.capture());
        MissaoBusca missaoSalva = captor.getValue();

        Assertions.assertEquals(new BigDecimal("150.00"), missaoSalva.getMediaPrecoHistorico());
        Assertions.assertEquals(new BigDecimal("100.00"), missaoSalva.getHistoricoDePrecos().get(0).getPrecoMinimo());

        Mockito.verify(notificacaoWhatsAppService)
                .processarGatilhosENotificar(Mockito.eq(missao), Mockito.eq(p1), Mockito.eq(new BigDecimal("150.00")), Mockito.nullable(BigDecimal.class));
    }

    @Test
    void executarMonitoramentoDiario_DeveAcionarHealthCheck_QuandoZeroResultados() throws Exception {
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("RTX 4060").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));
        Mockito.when(scraperService.buscarProdutos(missao)).thenReturn(List.of());

        workerService.executarMonitoramentoDiario();

        Mockito.verify(notificacaoWhatsAppService).notificarHealthCheckAdmin(Mockito.anyString());
    }

    @Test
    void executarMonitoramentoDiario_NaoDeveAcionarHealthCheck_QuandoTaxaForMenorQue30Porcento() throws Exception {
        MissaoBusca m1 = MissaoBusca.builder().termoDaBusca("M1").ativo(true).build();
        MissaoBusca m2 = MissaoBusca.builder().termoDaBusca("M2").ativo(true).build();
        MissaoBusca m3 = MissaoBusca.builder().termoDaBusca("M3").ativo(true).build();
        MissaoBusca m4 = MissaoBusca.builder().termoDaBusca("M4").ativo(true).build();

        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(m1, m2, m3, m4));

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link").build();

        Mockito.when(scraperService.buscarProdutos(Mockito.any(MissaoBusca.class)))
                .thenReturn(List.of(produto), List.of(produto), List.of(produto), List.of());

        workerService.executarMonitoramentoDiario();

        Mockito.verify(notificacaoWhatsAppService, Mockito.never()).notificarHealthCheckAdmin(Mockito.anyString());
    }

    @Test
    void executarMonitoramentoDiario_DeveAcionarHealthCheck_QuandoTaxaForMaiorOuIgual30Porcento() throws Exception {
        MissaoBusca m1 = MissaoBusca.builder().termoDaBusca("M1").ativo(true).build();
        MissaoBusca m2 = MissaoBusca.builder().termoDaBusca("M2").ativo(true).build();
        MissaoBusca m3 = MissaoBusca.builder().termoDaBusca("M3").ativo(true).build();

        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(m1, m2, m3));

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link").build();

        Mockito.when(scraperService.buscarProdutos(Mockito.any(MissaoBusca.class)))
                .thenReturn(List.of(produto), List.of(produto), List.of());

        workerService.executarMonitoramentoDiario();

        Mockito.verify(notificacaoWhatsAppService, Mockito.times(1)).notificarHealthCheckAdmin(Mockito.anyString());
    }

    @Test
    void executarMonitoramentoDiario_DeveAcionarHealthCheck_QuandoProxyFalharTresVezesSeguidas() throws Exception {
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("Teste de Proxy 403").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));

        Mockito.when(scraperService.buscarProdutos(missao))
                .thenThrow(new RuntimeException("Simulação de Erro HTTP 403 da ScraperAPI"));

        workerService.executarMonitoramentoDiario();

        Mockito.verify(notificacaoWhatsAppService, Mockito.times(1)).notificarHealthCheckAdmin(Mockito.anyString());
        Mockito.verify(scraperService, Mockito.times(3)).buscarProdutos(missao);
    }
}