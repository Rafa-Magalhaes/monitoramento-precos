package com.rafael.monitoramento_precos.infrastructure.scraping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import com.rafael.monitoramento_precos.infrastructure.scraping.dto.ProdutoScrapedDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoLivreScraperService {

    private final ObjectMapper objectMapper;

    @Value("${api.brightdata.host}")
    private String proxyHost;

    @Value("${api.brightdata.port}")
    private int proxyPort;

    @Value("${api.brightdata.username}")
    private String proxyUser;

    @Value("${api.brightdata.password}")
    private String proxyPassword;

    private static final String ML_BASE_URL = "https://lista.mercadolivre.com.br/";

    public List<ProdutoScrapedDTO> buscarProdutos(MissaoBusca missao) throws Exception {

        String urlML = ML_BASE_URL + formatarTermoParaUrl(missao.getTermoDaBusca());

        log.info("Iniciando scraping via Bright Data Web Unlocker na URL: {}", urlML);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        Document doc = Jsoup.connect(urlML)
                .proxy(proxy)
                .sslSocketFactory(socketFactory())
                .timeout(150000)
                .maxBodySize(0)
                .get();

        log.info("================ RAIOS-X DO SCRAPING ================");
        log.info("TÍTULO DA PÁGINA: {}", doc.title());
        log.info("HTML BRUTO: {}", doc.html());

        List<ProdutoScrapedDTO> produtosValidos = extrairViaJsonLd(doc, missao);

        if (produtosValidos.isEmpty()) {
            log.warn("JSON-LD ausente ou vazio. Acionando Fallback HTML (Plano B)...");
            produtosValidos = extrairViaFallbackHtml(doc, missao);
        }

        log.info("=====================================================");

        return produtosValidos;
    }

    private List<ProdutoScrapedDTO> extrairViaJsonLd(Document doc, MissaoBusca missao) {
        List<ProdutoScrapedDTO> encontrados = new ArrayList<>();
        Elements scripts = doc.select("script[type=application/ld+json]");

        for (Element script : scripts) {
            try {
                String json = script.data();

                if (json.contains("\"@type\":\"ItemList\"") || json.contains("\"@type\": \"ItemList\"")) {
                    JsonNode rootNode = objectMapper.readTree(json);
                    JsonNode itemListElement = rootNode.path("itemListElement");

                    if (itemListElement.isArray()) {
                        for (JsonNode itemNode : itemListElement) {
                            if (encontrados.size() >= 5) break;

                            JsonNode productNode = itemNode.path("item");
                            String titulo = productNode.path("name").asText("");
                            String link = productNode.path("url").asText("");
                            String precoStr = productNode.path("offers").path("price").asText("");

                            if (titulo.isBlank() || precoStr.isBlank() || link.isBlank()) continue;

                            if (isProdutoRelevante(titulo, missao.getPalavrasChaveExigidas(), missao.getPalavrasChaveProibidas())) {
                                encontrados.add(ProdutoScrapedDTO.builder()
                                        .titulo(titulo)
                                        .preco(new BigDecimal(precoStr))
                                        .linkProduto(link)
                                        .build());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Falha ao parsear bloco JSON-LD. Tentando próximo script...");
            }
        }
        return encontrados;
    }

    private List<ProdutoScrapedDTO> extrairViaFallbackHtml(Document doc, MissaoBusca missao) {
        List<ProdutoScrapedDTO> encontrados = new ArrayList<>();

        Elements cardsProdutos = doc.select("li.ui-search-layout__item, .poly-card");
        log.info("QTD DE CARDS ENCONTRADOS (Fallback HTML): {}", cardsProdutos.size());

        for (int i = 0; i < cardsProdutos.size(); i++) {
            Element card = cardsProdutos.get(i);

            if (encontrados.size() >= 5) break;

            String titulo = extrairTituloBlindado(card);
            String precoTexto = extrairTextoSeguro(card, ".andes-money-amount__fraction");
            String linkParcial = extrairLinkSeguro(card, "a");

            if (i == 0) {
                log.info("--- DEBUG DO PRIMEIRO PRODUTO EXTRAÍDO ---");
                log.info("TITULO ENCONTRADO: [{}]", titulo);
                log.info("PRECO ENCONTRADO: [{}]", precoTexto);
                log.info("LINK ENCONTRADO: [{}]", linkParcial);
                log.info("------------------------------------------");
            }

            if (titulo.isBlank() || precoTexto.isBlank() || linkParcial.isBlank()) continue;

            if (isProdutoRelevante(titulo, missao.getPalavrasChaveExigidas(), missao.getPalavrasChaveProibidas())) {
                BigDecimal precoTratado = converterPrecoParaBigDecimal(precoTexto);

                encontrados.add(ProdutoScrapedDTO.builder()
                        .titulo(titulo)
                        .preco(precoTratado)
                        .linkProduto(linkParcial)
                        .build());
            }
        }
        return encontrados;
    }

    private String formatarTermoParaUrl(String termo) {
        return termo.trim().toLowerCase().replaceAll("\\s+", "-");
    }

    private String extrairTextoSeguro(Element parent, String cssQuery) {
        Element element = parent.selectFirst(cssQuery);
        return element != null ? element.text() : "";
    }

    private String extrairLinkSeguro(Element parent, String cssQuery) {
        Element element = parent.selectFirst(cssQuery);
        return element != null ? element.attr("href") : "";
    }

    private boolean isProdutoRelevante(String tituloProduto, List<String> exigidas, List<String> proibidas) {
        String tituloLimpo = removerAcentos(tituloProduto.toUpperCase());

        for (String palavraProibida : proibidas) {
            String proibidaLimpa = removerAcentos(palavraProibida.toUpperCase());
            if (tituloLimpo.contains(proibidaLimpa)) {
                return false;
            }
        }

        for (String palavraExigida : exigidas) {
            String exigidaLimpa = removerAcentos(palavraExigida.toUpperCase());
            if (!tituloLimpo.contains(exigidaLimpa)) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal converterPrecoParaBigDecimal(String precoString) {
        try {
            String valorLimpo = precoString.replaceAll("[^0-9]", "");
            return new BigDecimal(valorLimpo);
        } catch (Exception e) {
            log.warn("Falha ao converter o preço: {}", precoString);
            return BigDecimal.ZERO;
        }
    }

    private String removerAcentos(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "");
    }

    private String extrairTituloBlindado(Element card) {
        String titulo = extrairTextoSeguro(card, ".poly-component__title, .ui-search-item__title");
        if (!titulo.isBlank()) return titulo;

        for (Element h2 : card.select("h2")) {
            if (!h2.text().isBlank()) return h2.text();
        }

        Element link = card.selectFirst("a");
        if (link != null && !link.text().isBlank()) {
            return link.text();
        }

        return "";
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criar SSLSocketFactory para o Proxy", e);
        }
    }

    @PostConstruct
    public void configurarProxyGlobal() {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingHost().equals(proxyHost)) {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
                return null;
            }
        });
    }
}