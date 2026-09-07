package com.rafael.monitoramento_precos.domain.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.monitoramento_precos.domain.enums.Role;
import com.rafael.monitoramento_precos.domain.model.Usuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "segredo-para-testes");
    }

    @Test
    void gerarToken_E_ValidarToken_CenarioFeliz() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("admin@teste.com")
                .role(Role.ROLE_ADMIN)
                .build();

        String token = tokenService.gerarToken(usuario);

        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isBlank());

        DecodedJWT jwtDecodificado = tokenService.validarToken(token);

        Assertions.assertNotNull(jwtDecodificado);
        Assertions.assertEquals("admin@teste.com", jwtDecodificado.getSubject());
        Assertions.assertEquals(usuario.getId().toString(), jwtDecodificado.getClaim("usuarioId").asString());
        Assertions.assertEquals("ROLE_ADMIN", jwtDecodificado.getClaim("role").asString());
    }

    @Test
    void validarToken_DeveRetornarNull_QuandoTokenInvalido() {
        DecodedJWT jwt = tokenService.validarToken("token.totalmente.falso");
        Assertions.assertNull(jwt);
    }
}