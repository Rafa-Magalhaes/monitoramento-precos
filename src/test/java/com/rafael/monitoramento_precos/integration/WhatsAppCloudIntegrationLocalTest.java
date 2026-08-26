package com.rafael.monitoramento_precos.integration;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.domain.service.NotificacaoWhatsAppService;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class WhatsAppCloudIntegrationLocalTest {

    @Autowired
    private NotificacaoWhatsAppService notificacaoWhatsAppService;

    // Utilizamos o MockitoBean (regra inegociável do Spring 3.4+) para não precisar bater no banco real
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void dispararMensagemRealParaMeuWhatsApp() {
        // 1. Mockamos o usuário para forçar o envio para o seu número real
        UUID meuId = UUID.randomUUID();
        Usuario eu = Usuario.builder()
                .id(meuId)
                .telefone("+5581992208217") // Seu número real aqui
                .build();

        when(usuarioRepository.findById(meuId)).thenReturn(Optional.of(eu));

        // 2. Montamos uma Missão e um Produto Fictícios
        MissaoBusca missao = MissaoBusca.builder()
                .usuarioId(meuId)
                .termoDaBusca("MacBook Pro M3")
                .precoAlvo(new BigDecimal("12000.00"))
                .build();

        ProdutoScrapedDTO produto = ProdutoScrapedDTO.builder()
                .titulo("MacBook Pro M3 512GB")
                .preco(new BigDecimal("11500.00")) // Preço menor que o alvo (Cenário A)
                .linkProduto("https://mercadolivre.com.br/macbook")
                .build();

        // 3. Acionamos o motor que chamará o Feign Client da Meta
        notificacaoWhatsAppService.processarGatilhosENotificar(
                missao,
                produto,
                new BigDecimal("15000.00"), // Média de hoje (ignorada no Cenário A)
                new BigDecimal("15500.00")  // Média antiga (ignorada no Cenário A)
        );

        // Se a configuração estiver correta, seu celular irá vibrar agora!
    }
}