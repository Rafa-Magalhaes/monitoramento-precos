package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Validated
@FeignClient(name = "whatsappClient", url = "${api.whatsapp.url:http://localhost:8080}")
public interface WhatsAppClient {

    @PostMapping("/waInstance{idInstance}/sendMessage/{apiTokenInstance}")
    void enviarMensagem(
            @PathVariable("idInstance") String idInstance,
            @PathVariable("apiTokenInstance") String apiTokenInstance,
            @RequestBody WhatsAppMessageRequestDTO request
    );

    // ENDPOINT DE VALIDAÇÃO ANTI-SPAM ---
    @PostMapping("/waInstance{idInstance}/checkWhatsapp/{apiTokenInstance}")
    CheckResponse checkWhatsapp(
            @PathVariable("idInstance") String idInstance,
            @PathVariable("apiTokenInstance") String apiTokenInstance,
            @RequestBody CheckRequest request
    );

    record CheckRequest(Long phoneNumber) {}
    record CheckResponse(boolean existsWhatsapp) {}
}