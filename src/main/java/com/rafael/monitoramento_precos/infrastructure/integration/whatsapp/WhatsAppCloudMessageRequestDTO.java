package com.rafael.monitoramento_precos.infrastructure.integration.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WhatsAppCloudMessageRequestDTO {

    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    private String to;

    @Builder.Default
    private String type = "template";

    private Template template;

    @Data
    @Builder
    public static class Template {
        private String name;
        private Language language;
        private List<Component> components;
    }

    @Data
    @Builder
    public static class Language {
        @Builder.Default
        private String code = "pt_BR";
    }

    @Data
    @Builder
    public static class Component {
        @Builder.Default
        private String type = "body";
        private List<Parameter> parameters;
    }

    @Data
    @Builder
    public static class Parameter {
        @Builder.Default
        private String type = "text";
        private String text;
    }
}