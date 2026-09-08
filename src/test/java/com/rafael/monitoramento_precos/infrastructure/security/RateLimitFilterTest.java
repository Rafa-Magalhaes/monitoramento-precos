package com.rafael.monitoramento_precos.infrastructure.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Bucket bucket;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.when(request.getRemoteAddr()).thenReturn("192.168.0.1");
    }

    @Test
    void doFilterInternal_DeveBloquearLogin_QuandoBucketEstiverVazio() throws Exception {
        // EXPLICAÇÃO MICRO: Configura o Mock do request para imitar um ataque de Força Bruta na rota de Login.
        Mockito.when(request.getRequestURI()).thenReturn("/auth/login");
        Mockito.when(request.getMethod()).thenReturn("POST");

        Mockito.when(rateLimitService.resolverBucketLogin("192.168.0.1")).thenReturn(bucket);
        // EXPLICAÇÃO MICRO: Simula que o balde secou (retornando false ao tentar consumir).
        Mockito.when(bucket.tryConsume(1)).thenReturn(false);

        StringWriter stringWriter = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // EXPLICAÇÃO MICRO: Valida se o status 429 foi acionado e garante que o filtro NÃO prosseguiu na cadeia.
        Mockito.verify(response).setStatus(429);
        Mockito.verify(filterChain, Mockito.never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DevePermitirRequisicao_QuandoBucketTiverTokens() throws Exception {
        Mockito.when(request.getRequestURI()).thenReturn("/auth/login");
        Mockito.when(request.getMethod()).thenReturn("POST");

        Mockito.when(rateLimitService.resolverBucketLogin("192.168.0.1")).thenReturn(bucket);
        // EXPLICAÇÃO MICRO: Balde tem saldo. O consumo retorna true.
        Mockito.when(bucket.tryConsume(1)).thenReturn(true);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // EXPLICAÇÃO MICRO: O filtro passa a requisição normalmente pelo FilterChain.
        Mockito.verify(filterChain).doFilter(request, response);
    }
}