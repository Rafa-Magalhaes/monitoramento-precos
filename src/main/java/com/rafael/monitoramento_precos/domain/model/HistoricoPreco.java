package com.rafael.monitoramento_precos.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoPreco {

    private BigDecimal precoMinimo;
    private BigDecimal precoMedio;
    private LocalDateTime dataCaptura;
}