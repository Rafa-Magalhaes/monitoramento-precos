package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Validated
@FeignClient(name = "whatsappClient", url = "${api.whatsapp.url:http://localhost:8080}")
public interface WhatsAppClient {

    @PostMapping("/sendText")
    void enviarMensagem(
            @RequestHeader("apikey") String apiKey,
            @RequestBody WhatsAppMessageRequestDTO request
    );
}