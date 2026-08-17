package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WhatsAppMessageRequestDTO {
    private String number;
    private String text;
}