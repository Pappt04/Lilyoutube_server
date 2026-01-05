package com.group17.lilyoutube_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.lilyoutube_server.model.AuthToken;
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

    public TokenAuthFilter(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // javne rute
        return path.startsWith("/api/auth")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization Bearer token");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> data = mapper.readValue(header.substring(7), Map.class);

        String token = data.get("token");

        AuthToken t = authTokenRepository.findByToken(token).orElse(null);
        if (t == null || t.getExpiresAt().isBefore(Instant.now())) {

            if(t!= null) authTokenRepository.deleteById(t.getId());

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                t.getUser().getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
