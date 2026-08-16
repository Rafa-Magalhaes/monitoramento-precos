package com.rafael.monitoramento_precos.infrastructure.scraping;

import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KabumScraperService {

    private static final String KABUM_BASE_URL = "https://www.kabum.com.br";
    private static final String KABUM_SEARCH_URL = KABUM_BASE_URL + "/busca/";

    public List<ProdutoScrapedDTO> buscarProdutos(MissaoBusca missao) {
        List<ProdutoScrapedDTO> produtosValidos = new ArrayList<>();

        try {
            String urlAlvo = KABUM_SEARCH_URL + formatarTermoParaUrl(missao.getTermoDaBusca());
            log.info("Iniciando scraping na URL: {}", urlAlvo);

            Document doc = Jsoup.connect(urlAlvo)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            Elements cardsProdutos = doc.select("div.productCard");

            for (Element card : cardsProdutos) {
                if (produtosValidos.size() >= 5) {
                    break;
                }

                String titulo = extrairTextoSeguro(card, "span.nameCard");
                String precoTexto = extrairTextoSeguro(card, "span.priceCard");
                // Captura a tag <a> que envelopa o produto e extrai a URL
                String linkParcial = extrairLinkSeguro(card, "a");

                if (titulo.isBlank() || precoTexto.isBlank() || linkParcial.isBlank()) {
                    continue;
                }

                if (isProdutoRelevante(titulo, missao.getPalavrasChaveExigidas(), missao.getPalavrasChaveProibidas())) {
                    BigDecimal precoTratado = converterPrecoParaBigDecimal(precoTexto);
                    String linkCompleto = KABUM_BASE_URL + linkParcial; // Monta o link clicável

                    produtosValidos.add(ProdutoScrapedDTO.builder()
                            .titulo(titulo)
                            .preco(precoTratado)
                            .linkProduto(linkCompleto)
                            .build());
                }
            }

        } catch (Exception e) {
            log.error("Erro ao realizar web scraping para o termo: {}", missao.getTermoDaBusca(), e);
        }

        return produtosValidos;
    }

    private String formatarTermoParaUrl(String termo) {
        return termo.trim().toLowerCase().replaceAll("\\s+", "-");
    }

    private String extrairTextoSeguro(Element parent, String cssQuery) {
        Element element = parent.selectFirst(cssQuery);
        return element != null ? element.text() : "";
    }

    // NOVO: Método que foca na propriedade "href" (o link) em vez do texto
    private String extrairLinkSeguro(Element parent, String cssQuery) {
        Element element = parent.selectFirst(cssQuery);
        return element != null ? element.attr("href") : "";
    }

    private boolean isProdutoRelevante(String tituloProduto, List<String> exigidas, List<String> proibidas) {
        String tituloFormatado = tituloProduto.toUpperCase();

        for (String palavraProibida : proibidas) {
            if (tituloFormatado.contains(palavraProibida.toUpperCase())) {
                return false;
            }
        }

        for (String palavraExigida : exigidas) {
            if (!tituloFormatado.contains(palavraExigida.toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal converterPrecoParaBigDecimal(String precoString) {
        String valorLimpo = precoString
                .replace("R$", "")
                .replace("&nbsp;", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        return new BigDecimal(valorLimpo);
    }
}