package com.rafael.monitoramento_precos.infrastructure.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID usuarioId;
    private final String email;

    public JwtAuthenticationToken(UUID usuarioId, String email, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.usuarioId = usuarioId;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.email;
    }
}