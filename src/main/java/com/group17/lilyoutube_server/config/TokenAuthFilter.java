package com.group17.lilyoutube_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.lilyoutube_server.model.AuthToken;
import com.group17.lilyoutube_server.monitoring.UserActivityMonitor;
import com.group17.lilyoutube_server.repository.AuthTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AuthTokenRepository authTokenRepository;
    private final UserActivityMonitor userActivityMonitor;
    private final ObjectMapper mapper;
  
    public TokenAuthFilter(AuthTokenRepository authTokenRepository, UserActivityMonitor userActivityMonitor, ObjectMapper mapper) {
        this.authTokenRepository = authTokenRepository;
        this.userActivityMonitor = userActivityMonitor;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {

            Map<String, String> data = mapper.readValue(header.substring(7), Map.class);
            String token = data.get("token");

            AuthToken t = authTokenRepository.findByToken(token).orElse(null);
            if (t != null && t.getExpiresAt().isAfter(Instant.now())) {
                var auth = new UsernamePasswordAuthenticationToken(
                        t.getUser().getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                userActivityMonitor.recordActivity(t.getUser().getEmail());
            } else if (t != null) {
                authTokenRepository.deleteById(t.getId());
            }
        } catch (Exception e) {
            // Unsuccessful authentication, proceed without authentication
        }

        chain.doFilter(request, response);
    }
}
