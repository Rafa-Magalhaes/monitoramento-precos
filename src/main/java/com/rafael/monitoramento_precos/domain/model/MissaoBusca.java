package com.rafael.monitoramento_precos.domain.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "missoes_busca")
@CompoundIndexes({
        @CompoundIndex(name = "uk_usuario_assinatura", def = "{'usuarioId': 1, 'assinaturaBusca': 1}", unique = true)
})
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

    private String termoDaBusca;

    private String assinaturaBusca;

    @Builder.Default
    private List<String> palavrasChaveExigidas = new ArrayList<>();

    @Builder.Default
    private List<String> palavrasChaveProibidas = new ArrayList<>();

    private BigDecimal precoAlvo;
    private BigDecimal mediaPrecoHistorico;

    @Builder.Default
    private List<HistoricoPreco> historicoDePrecos = new ArrayList<>();

    @Builder.Default
    private Boolean ativo = true;

    @CreatedDate
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;

    @LastModifiedDate
    private LocalDateTime dataAtualizacao;
}