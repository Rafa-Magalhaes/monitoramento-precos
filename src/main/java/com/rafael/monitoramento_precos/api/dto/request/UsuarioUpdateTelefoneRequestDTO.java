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
public class UsuarioUpdateTelefoneRequestDTO {

    @NotBlank(message = "O telefone não pode estar em branco")
    @Size(min = 10, max = 20, message = "O telefone deve ter entre 10 e 20 caracteres")
    private String telefone;
}