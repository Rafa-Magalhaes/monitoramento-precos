package com.rafael.monitoramento_precos.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissaoBuscaUpdateTermoRequestDTO {

    @NotBlank(message = "O novo termo da busca não pode estar em branco")
    @Size(max = 100, message = "O termo da busca deve ter no máximo 100 caracteres")
    private String termoDaBusca;
}