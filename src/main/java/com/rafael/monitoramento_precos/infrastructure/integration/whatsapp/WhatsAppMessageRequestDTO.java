package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WhatsAppMessageRequestDTO {

    // Identificador do chat no formato 55DDDNUMERO@c.us
    private String chatId;

    private String message;
}