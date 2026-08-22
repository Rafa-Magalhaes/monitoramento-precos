package com.rafael.monitoramento_precos.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "missoes_busca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class MissaoBusca {

    @Id
    private String id;

    private UUID usuarioId;


    // --- REGRAS DE RASTREAMENTO E FILTRAGEM ---

    private String termoDaBusca;

    @Builder.Default
    private List<String> palavrasChaveExigidas = new ArrayList<>();

    @Builder.Default
    private List<String> palavrasChaveProibidas = new ArrayList<>();


    // --- REGRAS DE NEGÓCIO E VALORES ---

    private BigDecimal precoAlvo;

    private BigDecimal mediaPrecoHistorico;

    @Builder.Default
    private List<HistoricoPreco> historicoDePrecos = new ArrayList<>();


    // --- AUDITORIA E CONTROLE ---

    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    private LocalDateTime dataCriacao;

    private LocalDateTime dataExpiracao;

    @LastModifiedDate
    private LocalDateTime dataAtualizacao;
}