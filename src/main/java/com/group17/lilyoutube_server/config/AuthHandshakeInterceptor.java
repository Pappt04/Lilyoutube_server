package com.group17.lilyoutube_server.config;

import com.group17.lilyoutube_server.model.AuthToken;
import com.group17.lilyoutube_server.repository.AuthTokenRepository;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthTokenRepository authTokenRepository;

    public AuthHandshakeInterceptor(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String token = servletRequest.getParameter("token");

            if (token != null && !token.isEmpty()) {
                AuthToken authToken = authTokenRepository.findByToken(token).orElse(null);

                if (authToken != null && authToken.getExpiresAt().isAfter(Instant.now())) {
                    String username = authToken.getUser().getUsername();
                    if (username == null || username.isEmpty()) {
                        username = authToken.getUser().getFirstName();
                    }
                    attributes.put("username", username);
                    return true;
                }
            }
        }
        return false; // Reject connection if no valid token
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
