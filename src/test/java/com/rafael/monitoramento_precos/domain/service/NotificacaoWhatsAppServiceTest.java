package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.HistoricoPreco;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppClient;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppMessageRequestDTO;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class NotificacaoWhatsAppServiceTest {

    @InjectMocks
    private NotificacaoWhatsAppService notificacaoWhatsAppService;

    @Mock
    private WhatsAppClient whatsAppClient;
    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        // Injeta as variáveis de ambiente (@Value) usando Reflection
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "idInstance", "testeId");
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "apiTokenInstance", "testeToken");
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "telefoneAdmin", "+5511999999999");
    }

    @Test
    void processarGatilhos_DeveEnviarCenarioA_ComRecordeHistorico_QuandoAtingirAlvo() {
        // Cenário (Arrange)
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().telefone("81 99999-9999").build();

        MissaoBusca missao = MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca("RTX 4060")
                .precoAlvo(new BigDecimal("1500.00"))
                .historicoDePrecos(List.of(
                        HistoricoPreco.builder().precoMinimo(new BigDecimal("1600.00")).build(),
                        HistoricoPreco.builder().precoMinimo(new BigDecimal("1400.00")).build() // O preço atual do dia
                ))
                .build();

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder()
                .preco(new BigDecimal("1400.00")) // Menor que o alvo (1500)
                .linkProduto("http://kabum.com/rtx4060")
                .build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // NOVO MOCK: Ensina o ator a responder que o número EXISTE para passar pela barreira Anti-Spam
        Mockito.when(whatsAppClient.checkWhatsapp(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(new WhatsAppClient.CheckResponse(true));

        // Ação (Act)
        // O último parâmetro (1600.00) simula qual era a média ontem
        notificacaoWhatsAppService.processarGatilhosENotificar(missao, produto, new BigDecimal("1800.00"), new BigDecimal("1600.00"));

        // Verificação (Assert)
        ArgumentCaptor<WhatsAppMessageRequestDTO> captor = ArgumentCaptor.forClass(WhatsAppMessageRequestDTO.class);
        Mockito.verify(whatsAppClient, Mockito.times(1))
                .enviarMensagem(Mockito.eq("testeId"), Mockito.eq("testeToken"), captor.capture());

        WhatsAppMessageRequestDTO payload = captor.getValue();

        // Verifica se formatou o DDI e adicionou o sufixo @c.us
        Assertions.assertEquals("5581999999999@c.us", payload.getChatId());
        Assertions.assertTrue(payload.getMessage().contains("RECORDE HISTÓRICO DE PREÇO BAIXO!"));
        Assertions.assertTrue(payload.getMessage().contains("R$ 1400.00"));
    }

    @Test
    void processarGatilhos_DeveEnviarCenarioB_QuandoPrecoMedioCair15Porcento() {
        // Cenário (Arrange)
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().telefone("81 99999-9999").build();

        // Média antiga era 2000. Para cair 15%, o novo preço médio deve ser <= 1700.
        BigDecimal mediaAntiga = new BigDecimal("2000.00");
        BigDecimal precoMedioDeHoje = new BigDecimal("1600.00"); // Caiu 20%, o gatilho DEVE disparar!

        MissaoBusca missao = MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca("Monitor Ultrawide")
                .precoAlvo(new BigDecimal("1000.00")) // O alvo não foi atingido
                .build();

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder()
                .preco(new BigDecimal("1500.00")) // Produto mais barato do dia (ainda acima do alvo)
                .linkProduto("http://link-do-monitor.com")
                .build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        Mockito.when(whatsAppClient.checkWhatsapp(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(new WhatsAppClient.CheckResponse(true));

        // Ação (Act)
        notificacaoWhatsAppService.processarGatilhosENotificar(missao, produto, precoMedioDeHoje, mediaAntiga);

        // Verificação (Assert)
        ArgumentCaptor<WhatsAppMessageRequestDTO> captor = ArgumentCaptor.forClass(WhatsAppMessageRequestDTO.class);
        Mockito.verify(whatsAppClient, Mockito.times(1))
                .enviarMensagem(Mockito.eq("testeId"), Mockito.eq("testeToken"), captor.capture());

        WhatsAppMessageRequestDTO payload = captor.getValue();

        // Verifica se a mensagem de oportunidade foi montada corretamente
        Assertions.assertTrue(payload.getMessage().contains("OPORTUNIDADE DE MERCADO!"));
        Assertions.assertTrue(payload.getMessage().contains("R$ 1600.00")); // Valida se inseriu o preço novo
        Assertions.assertTrue(payload.getMessage().contains("R$ 2000.00")); // Valida se inseriu o preço antigo
    }

    @Test
    void notificarHealthCheckAdmin_DeveEnviarMensagemParaOAdministrador() {
        // NOVO MOCK: Ensina o ator a responder que o número EXISTE
        Mockito.when(whatsAppClient.checkWhatsapp(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(new WhatsAppClient.CheckResponse(true));

        // Ação (Act)
        notificacaoWhatsAppService.notificarHealthCheckAdmin();

        // Verificação (Assert)
        ArgumentCaptor<WhatsAppMessageRequestDTO> captor = ArgumentCaptor.forClass(WhatsAppMessageRequestDTO.class);
        Mockito.verify(whatsAppClient, Mockito.times(1))
                .enviarMensagem(Mockito.anyString(), Mockito.anyString(), captor.capture());

        WhatsAppMessageRequestDTO payload = captor.getValue();
        Assertions.assertEquals("5511999999999@c.us", payload.getChatId());
        Assertions.assertTrue(payload.getMessage().contains("ALERTA CRÍTICO"));
    }

    @Test
    void alternarNonoDigito_DeveAdicionarOuRemoverO9DigitoCorretamente() {
        // Cenário 1: Número com 13 dígitos (Possui o 9). O sistema DEVE remover.
        String numeroCom9 = "5511999999999";
        String resultadoSem9 = ReflectionTestUtils.invokeMethod(notificacaoWhatsAppService, "alternarNonoDigito", numeroCom9);
        Assertions.assertEquals("551199999999", resultadoSem9);

        // Cenário 2: Número com 12 dígitos (Não possui o 9). O sistema DEVE adicionar.
        String numeroSem9 = "558199999999";
        String resultadoCom9 = ReflectionTestUtils.invokeMethod(notificacaoWhatsAppService, "alternarNonoDigito", numeroSem9);
        Assertions.assertEquals("5581999999999", resultadoCom9);

        // Cenário 3: Número fora do padrão brasileiro (muito curto ou longo). O sistema DEVE ignorar e retornar null.
        String numeroInvalido = "558199";
        String resultadoNulo = ReflectionTestUtils.invokeMethod(notificacaoWhatsAppService, "alternarNonoDigito", numeroInvalido);
        Assertions.assertNull(resultadoNulo);
    }
}