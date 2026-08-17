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

    @Value("${api.whatsapp.apikey:ChaveFalsaParaTestes}")
    private String apiKey;

    @Value("${app.admin.telefone:+5511999999999}")
    private String telefoneAdmin;

    public void processarGatilhosENotificar(MissaoBusca missao, ProdutoScrapedDTO produtoMaisBarato, BigDecimal precoMedioAtual) {

        Usuario usuario = usuarioRepository.findById(missao.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para envio de notificação."));

        // Gatilho A: Alvo Atingido (Preço Mínimo <= Preço Alvo)
        if (produtoMaisBarato.getPreco().compareTo(missao.getPrecoAlvo()) <= 0) {
            boolean isRecorde = verificarRecordeHistorico(missao, produtoMaisBarato.getPreco());
            String mensagem = montarMensagemCenarioA(missao, produtoMaisBarato, isRecorde);

            disparar(usuario.getTelefone(), mensagem);
            return; // Se atingiu o alvo principal, não envia a notificação de média (Cenário B) para não floodar o usuário.
        }

        // Gatilho B: Oportunidade de Mercado (Queda >= 15% da média histórica)
        if (missao.getMediaPrecoHistorico() != null && missao.getMediaPrecoHistorico().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal limiteOportunidade = missao.getMediaPrecoHistorico().multiply(new BigDecimal("0.85"));

            if (precoMedioAtual.compareTo(limiteOportunidade) <= 0) {
                String mensagem = montarMensagemCenarioB(missao, precoMedioAtual);
                disparar(usuario.getTelefone(), mensagem);
            }
        }
    }

    public void notificarHealthCheckAdmin() {
        String mensagem = "🚨 ALERTA CRÍTICO: O Motor de Scraping retornou 0 resultados. Possível alteração de layout na Kabum ou IP bloqueado!";
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
                "Acesse a Kabum e confira as ofertas na vitrine.";
    }

    private void disparar(String numero, String texto) {
        try {
            WhatsAppMessageRequestDTO payload = WhatsAppMessageRequestDTO.builder()
                    .number(numero)
                    .text(texto)
                    .build();

            whatsAppClient.enviarMensagem(apiKey, payload);
            log.info("Notificação enviada com sucesso para o número: {}", numero);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação para o número {}: {}", numero, e.getMessage());
        }
    }
}