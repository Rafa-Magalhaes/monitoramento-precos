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
        MissaoBusca missao = MissaoBusca.builder().termoDaBusca("RTX 4060").ativo(true).build();
        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(missao));
        Mockito.when(scraperService.buscarProdutos(missao)).thenReturn(List.of());

        // Ação
        workerService.executarMonitoramentoDiario();


        Mockito.verify(notificacaoWhatsAppService).notificarHealthCheckAdmin(Mockito.anyString());
    }

    @Test
    void executarMonitoramentoDiario_NaoDeveAcionarHealthCheck_QuandoTaxaForMenorQue30Porcento() throws Exception {
        // Cenário (Fronteira Inferior): 4 missões totais. 3 com sucesso (produtos) e 1 com falha (vazia).
        // Cálculo esperado: 1 / 4 = 0.25 (25%). O alarme NÃO deve tocar.
        MissaoBusca m1 = MissaoBusca.builder().termoDaBusca("M1").ativo(true).build();
        MissaoBusca m2 = MissaoBusca.builder().termoDaBusca("M2").ativo(true).build();
        MissaoBusca m3 = MissaoBusca.builder().termoDaBusca("M3").ativo(true).build();
        MissaoBusca m4 = MissaoBusca.builder().termoDaBusca("M4").ativo(true).build();

        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(m1, m2, m3, m4));

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link").build();

        // Truque Sênior do Mockito: Podemos passar múltiplos retornos para a mesma chamada!
        // Ele vai retornar Lista Cheia nas 3 primeiras vezes, e Lista Vazia na 4ª vez.
        Mockito.when(scraperService.buscarProdutos(Mockito.any(MissaoBusca.class)))
                .thenReturn(List.of(produto), List.of(produto), List.of(produto), List.of());

        // Ação
        workerService.executarMonitoramentoDiario();

        // Verificação: Usamos o Mockito.never() para garantir que o metodo de disparo NUNCA foi acionado
        Mockito.verify(notificacaoWhatsAppService, Mockito.never()).notificarHealthCheckAdmin(Mockito.anyString());
    }

    @Test
    void executarMonitoramentoDiario_DeveAcionarHealthCheck_QuandoTaxaForMaiorOuIgual30Porcento() throws Exception {
        // Cenário (Fronteira Superior): 3 missões totais. 2 com sucesso e 1 com falha.
        // Cálculo esperado: 1 / 3 = 0.33 (33.3%). O alarme DEVE tocar.
        MissaoBusca m1 = MissaoBusca.builder().termoDaBusca("M1").ativo(true).build();
        MissaoBusca m2 = MissaoBusca.builder().termoDaBusca("M2").ativo(true).build();
        MissaoBusca m3 = MissaoBusca.builder().termoDaBusca("M3").ativo(true).build();

        Mockito.when(missaoBuscaRepository.findByAtivoTrue()).thenReturn(List.of(m1, m2, m3));

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder().preco(new BigDecimal("100.00")).linkProduto("link").build();

        // Retorna Lista Cheia nas 2 primeiras chamadas, e Lista Vazia na 3ª chamada.
        Mockito.when(scraperService.buscarProdutos(Mockito.any(MissaoBusca.class)))
                .thenReturn(List.of(produto), List.of(produto), List.of());

        // Ação
        workerService.executarMonitoramentoDiario();

        // Verificação: Garantimos que o alarme tocou exata 1 vez
        Mockito.verify(notificacaoWhatsAppService, Mockito.times(1)).notificarHealthCheckAdmin(Mockito.anyString());
    }
}