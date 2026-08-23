package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppCloudClient;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppCloudMessageRequestDTO;
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
    private WhatsAppCloudClient whatsAppClient;
    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "phoneNumberId", "idFicticio");
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "accessToken", "tokenSecreto");
        ReflectionTestUtils.setField(notificacaoWhatsAppService, "telefoneAdmin", "+5511999999999");
    }

    @Test
    void processarGatilhos_DeveEnviarTemplateAlertaAtingido_CenarioA() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().telefone("81 99999-9999").build();

        MissaoBusca missao = MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca("RTX 4060")
                .precoAlvo(new BigDecimal("1500.00"))
                .historicoDePrecos(List.of())
                .build();

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder()
                .preco(new BigDecimal("1400.00"))
                .linkProduto("http://kabum.com/rtx4060")
                .build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        notificacaoWhatsAppService.processarGatilhosENotificar(missao, produto, new BigDecimal("1800.00"), new BigDecimal("1600.00"));

        ArgumentCaptor<WhatsAppCloudMessageRequestDTO> captor = ArgumentCaptor.forClass(WhatsAppCloudMessageRequestDTO.class);
        Mockito.verify(whatsAppClient, Mockito.times(1))
                .enviarMensagemTemplate(Mockito.eq("idFicticio"), Mockito.eq("Bearer tokenSecreto"), captor.capture());

        WhatsAppCloudMessageRequestDTO payload = captor.getValue();

        Assertions.assertEquals("5581999999999", payload.getTo());
        Assertions.assertEquals("alerta_preco_atingido", payload.getTemplate().getName());

        List<WhatsAppCloudMessageRequestDTO.Parameter> params = payload.getTemplate().getComponents().get(0).getParameters();
        Assertions.assertEquals("RTX 4060", params.get(0).getText());
        Assertions.assertEquals("1400.00", params.get(1).getText());
        Assertions.assertEquals("http://kabum.com/rtx4060", params.get(2).getText());
    }

    @Test
    void processarGatilhos_DeveEnviarTemplateOportunidade_CenarioB() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder().telefone("81 99999-9999").build();

        BigDecimal mediaAntiga = new BigDecimal("2000.00");
        BigDecimal precoMedioDeHoje = new BigDecimal("1600.00");

        MissaoBusca missao = MissaoBusca.builder()
                .usuarioId(usuarioId)
                .termoDaBusca("Monitor Ultrawide")
                .precoAlvo(new BigDecimal("1000.00"))
                .build();

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder()
                .preco(new BigDecimal("1500.00"))
                .linkProduto("http://link-do-monitor.com")
                .build();

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        notificacaoWhatsAppService.processarGatilhosENotificar(missao, produto, precoMedioDeHoje, mediaAntiga);

        ArgumentCaptor<WhatsAppCloudMessageRequestDTO> captor = ArgumentCaptor.forClass(WhatsAppCloudMessageRequestDTO.class);
        Mockito.verify(whatsAppClient, Mockito.times(1))
                .enviarMensagemTemplate(Mockito.eq("idFicticio"), Mockito.eq("Bearer tokenSecreto"), captor.capture());

        WhatsAppCloudMessageRequestDTO payload = captor.getValue();

        Assertions.assertEquals("alerta_preco_queda", payload.getTemplate().getName());

        List<WhatsAppCloudMessageRequestDTO.Parameter> params = payload.getTemplate().getComponents().get(0).getParameters();
        Assertions.assertEquals("Monitor Ultrawide", params.get(0).getText());
        Assertions.assertEquals("2000.00", params.get(1).getText());
        Assertions.assertEquals("1600.00", params.get(2).getText());
    }
}