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
}