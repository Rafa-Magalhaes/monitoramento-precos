package com.rafael.monitoramento_precos.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissaoBuscaCreateRequestDTO {

    @NotBlank(message = "O termo da busca não pode estar em branco")
    @Size(max = 100, message = "O termo da busca deve ter no máximo 100 caracteres")
    private String termoDaBusca;

    @NotNull(message = "O preço alvo é obrigatório")
    @Positive(message = "O preço alvo deve ser maior que zero")
    private BigDecimal precoAlvo;

    // Campo Opcional conforme a sua definição de negócio
    private List<String> palavrasChaveProibidas;

}