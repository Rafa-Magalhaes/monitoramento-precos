package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "whatsAppCloudClient",
        url = "${api.whatsapp.cloud.url:https://graph.facebook.com/v19.0}"
)
public interface WhatsAppCloudClient {

    @PostMapping("/{phoneNumberId}/messages")
    void enviarMensagemTemplate(
            @PathVariable("phoneNumberId") String phoneNumberId,
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody WhatsAppCloudMessageRequestDTO request
    );
}