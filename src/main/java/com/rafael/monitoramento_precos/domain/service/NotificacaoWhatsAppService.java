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

    public void processarGatilhosENotificar(MissaoBusca missao, ProdutoScrapedDTO produtoMaisBarato, BigDecimal precoMedioAtual, BigDecimal mediaHistoricaAntiga) {

        Usuario usuario = usuarioRepository.findById(missao.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para envio de notificação."));

        if (produtoMaisBarato.getPreco().compareTo(missao.getPrecoAlvo()) <= 0) {
            boolean isRecorde = verificarRecordeHistorico(missao, produtoMaisBarato.getPreco());
            String mensagem = montarMensagemCenarioA(missao, produtoMaisBarato, isRecorde);
            disparar(usuario.getTelefone(), mensagem);
            return;
        }

        if (mediaHistoricaAntiga != null && mediaHistoricaAntiga.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal limiteOportunidade = mediaHistoricaAntiga.multiply(new BigDecimal("0.85"));

            if (precoMedioAtual.compareTo(limiteOportunidade) <= 0) {
                String mensagem = montarMensagemCenarioB(missao, precoMedioAtual, mediaHistoricaAntiga);
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

    private String montarMensagemCenarioB(MissaoBusca missao, BigDecimal precoMedioAtual, BigDecimal mediaHistoricaAntiga) {
        return "📉 *OPORTUNIDADE DE MERCADO!* 📉\n\n" +
                "Notamos uma queda generalizada nos preços para *" + missao.getTermoDaBusca() + "*.\n" +
                "A média histórica era R$ " + mediaHistoricaAntiga.setScale(2, RoundingMode.HALF_UP) + "\n" +
                "A média de hoje caiu para *R$ " + precoMedioAtual.setScale(2, RoundingMode.HALF_UP) + "*!\n\n" +
                "Acesse o Mercado Livre e confira as ofertas na vitrine.";
    }

    private void disparar(String numeroOriginal, String texto) {
        String numeroLimpo = numeroOriginal.replaceAll("\\D", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        boolean existe = numeroExisteNoWhatsApp(numeroLimpo);
        String numeroValidado = numeroLimpo;

        if (!existe) {
            log.warn("Número {} não encontrado na base do WhatsApp. Aplicando Fallback do 9º dígito...", numeroLimpo);
            String numeroMutante = alternarNonoDigito(numeroLimpo);

            if (numeroMutante != null && numeroExisteNoWhatsApp(numeroMutante)) {
                numeroValidado = numeroMutante;
                log.info("Fallback bem sucedido! O JID real na Meta é: {}", numeroValidado);
            } else {
                log.error("Abordagem abortada para evitar bloqueio de SPAM. Nenhuma variação do número existe: {}", numeroOriginal);
                return; // Morre aqui. Protegemos nossa API de banimento!
            }
        }

        try {
            String chatIdFinal = numeroValidado + "@c.us";
            WhatsAppMessageRequestDTO payload = WhatsAppMessageRequestDTO.builder()
                    .chatId(chatIdFinal)
                    .message(texto)
                    .build();

            whatsAppClient.enviarMensagem(idInstance, apiTokenInstance, payload);
            log.info("Notificação enviada com sucesso para o chatId: {}", chatIdFinal);
        } catch (Exception e) {
            log.error("Falha inesperada no servidor da Green API: {}", e.getMessage());
        }
    }

    private boolean numeroExisteNoWhatsApp(String numeroSomenteDigitos) {
        try {
            WhatsAppClient.CheckRequest request = new WhatsAppClient.CheckRequest(Long.parseLong(numeroSomenteDigitos));
            WhatsAppClient.CheckResponse response = whatsAppClient.checkWhatsapp(idInstance, apiTokenInstance, request);
            return response.existsWhatsapp();
        } catch (Exception e) {
            log.warn("Falha ao consultar validade do número na Green API: {}", e.getMessage());
            return false;
        }
    }

    private String alternarNonoDigito(String numero) {
        // Se tem 13 dígitos (55 + DDD + 9 números), retiramos o 9
        if (numero.length() == 13) {
            return numero.substring(0, 4) + numero.substring(5);
        }
        // Se tem 12 dígitos (55 + DDD + 8 números), adicionamos o 9 após o DDD
        else if (numero.length() == 12) {
            return numero.substring(0, 4) + "9" + numero.substring(4);
        }
        return null;
    }
}