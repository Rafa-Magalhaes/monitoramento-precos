package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppCloudClient;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppCloudMessageRequestDTO;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoWhatsAppService {

    private final WhatsAppCloudClient whatsAppClient;
    private final UsuarioRepository usuarioRepository;

    @Value("${api.whatsapp.cloud.phone-number-id:123456789}")
    private String phoneNumberId;

    @Value("${api.whatsapp.cloud.access-token:token_provisorio_teste}")
    private String accessToken;

    @Value("${app.admin.telefone:+5511999999999}")
    private String telefoneAdmin;

    public void processarGatilhosENotificar(MissaoBusca missao, ProdutoScrapedDTO produtoMaisBarato, BigDecimal precoMedioAtual, BigDecimal mediaHistoricaAntiga) {

        Usuario usuario = usuarioRepository.findById(missao.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para envio de notificação."));

        if (produtoMaisBarato.getPreco().compareTo(missao.getPrecoAlvo()) <= 0) {
            // Cenário A: Alvo Atingido
            dispararTemplate(usuario.getTelefone(), "alerta_preco_atingido", List.of(
                    missao.getTermoDaBusca(),
                    produtoMaisBarato.getPreco().toString(),
                    produtoMaisBarato.getLinkProduto()
            ));
            return;
        }

        if (mediaHistoricaAntiga != null && mediaHistoricaAntiga.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal limiteOportunidade = mediaHistoricaAntiga.multiply(new BigDecimal("0.85"));

            if (precoMedioAtual.compareTo(limiteOportunidade) <= 0) {
                // Cenário B: Queda de 15%
                dispararTemplate(usuario.getTelefone(), "alerta_preco_queda", List.of(
                        missao.getTermoDaBusca(),
                        mediaHistoricaAntiga.setScale(2, RoundingMode.HALF_UP).toString(),
                        precoMedioAtual.setScale(2, RoundingMode.HALF_UP).toString()
                ));
            }
        }
    }

    public void notificarHealthCheckAdmin() {
        dispararTemplate(telefoneAdmin, "alerta_health_check", List.of(
                "O motor de scraping retornou 0 resultados. Possível alteração de layout ou IP bloqueado na loja."
        ));
    }

    private void dispararTemplate(String numeroOriginal, String templateName, List<String> variaveis) {
        String numeroLimpo = numeroOriginal.replaceAll("\\D", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        // Transforma a lista de Strings Java no modelo de Parâmetros exigido pela Meta
        List<WhatsAppCloudMessageRequestDTO.Parameter> parameters = variaveis.stream()
                .map(valor -> WhatsAppCloudMessageRequestDTO.Parameter.builder().text(valor).build())
                .toList();

        WhatsAppCloudMessageRequestDTO.Component component = WhatsAppCloudMessageRequestDTO.Component.builder()
                .parameters(parameters)
                .build();

        WhatsAppCloudMessageRequestDTO.Template template = WhatsAppCloudMessageRequestDTO.Template.builder()
                .name(templateName)
                .language(WhatsAppCloudMessageRequestDTO.Language.builder().code("pt_BR").build())
                .components(parameters.isEmpty() ? List.of() : List.of(component))
                .build();

        WhatsAppCloudMessageRequestDTO payload = WhatsAppCloudMessageRequestDTO.builder()
                .to(numeroLimpo)
                .template(template)
                .build();

        try {
            // A palavra "Bearer " é o padrão OAuth 2.0 exigido pela Meta
            whatsAppClient.enviarMensagemTemplate(phoneNumberId, "Bearer " + accessToken, payload);
            log.info("Template {} enviado com sucesso para a fila da Meta. Destino: {}", templateName, numeroLimpo);
        } catch (Exception e) {
            log.error("Falha inesperada ao enviar template para a Meta API: {}", e.getMessage());
        }
    }
}