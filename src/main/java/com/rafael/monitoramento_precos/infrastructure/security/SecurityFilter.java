package com.rafael.monitoramento_precos.infrastructure.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.rafael.monitoramento_precos.domain.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = recuperarToken(request);

        if (token != null) {
            DecodedJWT decodedJWT = tokenService.validarToken(token);

            if (decodedJWT != null) {
                String subject = decodedJWT.getSubject();
                String role = decodedJWT.getClaim("role").asString();
                UUID usuarioId = UUID.fromString(decodedJWT.getClaim("usuarioId").asString());

                var authorities = List.of(new SimpleGrantedAuthority(role));

                var authentication = new JwtAuthenticationToken(usuarioId, subject, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}