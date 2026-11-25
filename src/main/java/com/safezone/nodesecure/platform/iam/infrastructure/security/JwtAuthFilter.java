package com.safezone.nodesecure.platform.iam.infrastructure.security;

import com.safezone.nodesecure.platform.iam.application.internal.sessions.SessionUserResolver;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the bearer token and loads the current user from the database.
 * Granted authorities: the role (e.g. ROLE_OWNER) plus every effective permission code.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final SessionUserResolver sessionUserResolver;

    public JwtAuthFilter(TokenService tokenService, SessionUserResolver sessionUserResolver) {
        this.tokenService = tokenService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            tokenService.readToken(header.substring(PREFIX.length()))
                    .flatMap(sessionUserResolver::resolve)
                    .ifPresent(this::authenticate);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(SessionUser session) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(session.role().name()));
        for (Permission permission : session.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission.code()));
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(session, null, authorities));
    }
}
