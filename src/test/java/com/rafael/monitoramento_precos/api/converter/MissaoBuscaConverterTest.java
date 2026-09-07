package com.rafael.monitoramento_precos.api.converter;

import com.rafael.monitoramento_precos.api.dto.request.MissaoBuscaCreateRequestDTO;
import com.rafael.monitoramento_precos.domain.model.MissaoBusca;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class MissaoBuscaConverterTest {

    private MissaoBuscaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MissaoBuscaConverter();
    }

    @Test
    void toEntity_DeveGerarAssinaturaDeBuscaOrdenada_CenarioFeliz() {
        // Cenário (Arrange)
        UUID id = UUID.randomUUID();
        MissaoBuscaCreateRequestDTO dto = MissaoBuscaCreateRequestDTO.builder()
                .termoDaBusca("Zebra de Pelucia Amarela")
                .build();

        // Ação (Act)
        // EXPLICAÇÃO MICRO: Invoca a entidade para simular a criação.
        MissaoBusca entidade = converter.toEntity(dto, id);

        // Verificação (Assert)
        // EXPLICAÇÃO MICRO: Garante que as palavras filtradas pelo extrairPalavrasChave
        // (AMARELA, PELUCIA, ZEBRA) foram unidas exatamente em ordem alfabética.
        Assertions.assertEquals("AMARELA-PELUCIA-ZEBRA", entidade.getAssinaturaBusca());
    }
    
    @Test
    void extrairPalavrasChave_DeveLimparFatiarEFiltrarStopWords() {
        // Cenário (Arrange)
        String termoSujo = "  iPhone 15, Pro-Max! de 256GB  ";

        // Ação (Act)
        List<String> resultado = converter.extrairPalavrasChave(termoSujo);

        // Verificação (Assert)
        Assertions.assertEquals(4, resultado.size());
        Assertions.assertTrue(resultado.contains("IPHONE"));
        Assertions.assertTrue(resultado.contains("PRO"));
        Assertions.assertTrue(resultado.contains("MAX"));
        Assertions.assertTrue(resultado.contains("256GB"));

        // Garante que a Stop Word "de" (2 letras) foi ignorada
        Assertions.assertFalse(resultado.contains("DE"));
    }
}