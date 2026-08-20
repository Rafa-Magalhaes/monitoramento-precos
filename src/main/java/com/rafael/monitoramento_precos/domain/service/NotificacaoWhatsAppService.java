package com.rafael.monitoramento_precos.domain.service;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppClient;
import com.rafael.monitoramento_precos.infrastructure.integration.whatsapp.WhatsAppMessageRequestDTO;
import com.rafael.monitoramento_precos.infrastructure.repository.UsuarioRepository;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoWhatsAppService {

    private final WhatsAppClient whatsAppClient;
    private final UsuarioRepository usuarioRepository;

    @Value("${api.whatsapp.id-instance:id-falso-teste}")
    private String idInstance;

    @Value("${api.whatsapp.api-token-instance:token-falso-teste}")
    private String apiTokenInstance;

    @Value("${app.admin.telefone:+5511999999999}")
    private String telefoneAdmin;

    public void processarGatilhosENotificar(MissaoBusca missao, ProdutoScrapedDTO produtoMaisBarato, BigDecimal precoMedioAtual) {

        Usuario usuario = usuarioRepository.findById(missao.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para envio de notificação."));

        if (produtoMaisBarato.getPreco().compareTo(missao.getPrecoAlvo()) <= 0) {
            boolean isRecorde = verificarRecordeHistorico(missao, produtoMaisBarato.getPreco());
            String mensagem = montarMensagemCenarioA(missao, produtoMaisBarato, isRecorde);

            disparar(usuario.getTelefone(), mensagem);
            return;
        }

        if (missao.getMediaPrecoHistorico() != null && missao.getMediaPrecoHistorico().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal limiteOportunidade = missao.getMediaPrecoHistorico().multiply(new BigDecimal("0.85"));

            if (precoMedioAtual.compareTo(limiteOportunidade) <= 0) {
                String mensagem = montarMensagemCenarioB(missao, precoMedioAtual);
                disparar(usuario.getTelefone(), mensagem);
            }
        }
    }

    public void notificarHealthCheckAdmin() {
        String mensagem = "🚨 ALERTA CRÍTICO: O Motor de Scraping retornou 0 resultados. Possível alteração de layout ou IP bloqueado!";
        disparar(telefoneAdmin, mensagem);
    }

    private boolean verificarRecordeHistorico(MissaoBusca missao, BigDecimal precoAtual) {
        long qtdPrecosNoMesmoPatamar = missao.getHistoricoDePrecos().stream()
                .filter(h -> h.getPrecoMinimo().compareTo(precoAtual) <= 0)
                .count();
        return qtdPrecosNoMesmoPatamar == 1;
    }

    private String montarMensagemCenarioA(MissaoBusca missao, ProdutoScrapedDTO produto, boolean isRecorde) {
        StringBuilder sb = new StringBuilder();
        if (isRecorde) {
            sb.append("🚨 *RECORDE HISTÓRICO DE PREÇO BAIXO!* 🚨\n\n");
        } else {
            sb.append("🎯 *ALVO ATINGIDO!* 🎯\n\n");
        }
        sb.append("O item *").append(missao.getTermoDaBusca()).append("* atingiu a sua meta!\n")
                .append("Preço Encontrado: *R$ ").append(produto.getPreco()).append("*\n\n")
                .append("🔗 Compre agora antes que acabe:\n")
                .append(produto.getLinkProduto());
        return sb.toString();
    }

    private String montarMensagemCenarioB(MissaoBusca missao, BigDecimal precoMedioAtual) {
        return "📉 *OPORTUNIDADE DE MERCADO!* 📉\n\n" +
                "Notamos uma queda generalizada nos preços para *" + missao.getTermoDaBusca() + "*.\n" +
                "A média histórica era R$ " + missao.getMediaPrecoHistorico().setScale(2, RoundingMode.HALF_UP) + "\n" +
                "A média de hoje caiu para *R$ " + precoMedioAtual.setScale(2, RoundingMode.HALF_UP) + "*!\n\n" +
                "Acesse o Mercado Livre e confira as ofertas na vitrine.";
    }

    private void disparar(String numeroOriginal, String texto) {
        try {
            String chatIdFormatado = formatarNumeroParaChatId(numeroOriginal);

            WhatsAppMessageRequestDTO payload = WhatsAppMessageRequestDTO.builder()
                    .chatId(chatIdFormatado)
                    .message(texto)
                    .build();

            whatsAppClient.enviarMensagem(idInstance, apiTokenInstance, payload);
            log.info("Notificação enviada com sucesso via Green API para o chatId: {}", chatIdFormatado);

        } catch (Exception e) {
            log.error("Falha crítica ao enviar notificação para o número {}: {}", numeroOriginal, e.getMessage());
        }
    }

    private String formatarNumeroParaChatId(String telefone) {
        // 1. Remove qualquer caractere que não seja número (ex: parênteses, espaços, traços)
        String apenasDigitos = telefone.replaceAll("\\D", "");

        // 2. Se o número não começar com 55 (código do Brasil), nós adicionamos
        if (!apenasDigitos.startsWith("55")) {
            apenasDigitos = "55" + apenasDigitos;
        }

        // 3. Concatena com o sufixo obrigatório da Green API
        return apenasDigitos + "@c.us";
    }
}