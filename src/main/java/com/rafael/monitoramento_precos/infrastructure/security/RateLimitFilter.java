package com.rafael.monitoramento_precos.infrastructure.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // EXPLICAÇÃO MICRO: Capturamos a rota, o método HTTP e o IP real do cliente para roteamento das restrições.
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ipCliente = request.getRemoteAddr();

        if (uri.equals("/auth/login") && method.equals("POST")) {
            Bucket bucket = rateLimitService.resolverBucketLogin(ipCliente);
            // EXPLICAÇÃO MICRO: tryConsume(1) tenta retirar 1 token. Se retornar false, o balde esvaziou.
            if (!bucket.tryConsume(1)) {
                bloquearRequisicao(response, "Muitas tentativas de login. Aguarde 15 minutos e tente novamente.");
                return;
            }
        }

        if (uri.equals("/missoes") && method.equals("POST")) {
            Bucket bucket = rateLimitService.resolverBucketCriacaoMissao(ipCliente);
            if (!bucket.tryConsume(1)) {
                bloquearRequisicao(response, "Você está criando missões rápido demais. Aguarde 1 minuto.");
                return;
            }
        }

        if (uri.equals("/usuarios") && method.equals("POST")) {
            Bucket bucket = rateLimitService.resolverBucketCadastro(ipCliente);
            if (!bucket.tryConsume(1)) {
                bloquearRequisicao(response, "Limite de criação de contas atingido para este IP. Tente novamente mais tarde.");
                return;
            }
        }

        // EXPLICAÇÃO MICRO: Se o balde tiver tokens, doFilter passa a requisição adiante para a camada do Spring Security.
        filterChain.doFilter(request, response);
    }

    private void bloquearRequisicao(HttpServletResponse response, String mensagem) throws IOException {
        // EXPLICAÇÃO MICRO: Monta manualmente uma resposta de erro HTTP 429 (Too Many Requests) devolvendo um JSON limpo.
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(String.format("{\"erro\": \"%s\"}", mensagem));
    }
}