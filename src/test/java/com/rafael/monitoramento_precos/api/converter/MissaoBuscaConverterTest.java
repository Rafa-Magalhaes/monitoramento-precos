package com.rafael.monitoramento_precos.api.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class MissaoBuscaConverterTest {

    private MissaoBuscaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MissaoBuscaConverter();
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