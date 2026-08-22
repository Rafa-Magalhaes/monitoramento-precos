package com.rafael.monitoramento_precos.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MissaoBuscaResponseDTO {

    private String id;
    private String termoDaBusca;
    private BigDecimal precoAlvo;
    private List<String> palavrasChaveExigidas;
    private List<String> palavrasChaveProibidas;
    private Boolean ativo;
    private LocalDateTime dataCriacao;

}