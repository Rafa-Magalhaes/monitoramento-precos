package com.rafael.monitoramento_precos.infrastructure.scraping.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder
@ToString // Ajuda o desenvolvedor a ler os logs no console!
public class ProdutoScrapedDTO {
    private String titulo;
    private BigDecimal preco;
    private String linkProduto;
}