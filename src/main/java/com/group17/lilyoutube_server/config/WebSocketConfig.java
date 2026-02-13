package com.group17.lilyoutube_server.config;

import com.group17.lilyoutube_server.handler.ChatWebSocketHandler;
import com.group17.lilyoutube_server.handler.WatchPartyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WatchPartyWebSocketHandler watchPartyWebSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
            WatchPartyWebSocketHandler watchPartyWebSocketHandler,
            AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.watchPartyWebSocketHandler = watchPartyWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/api/stream/*/chat")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");

        // Watch Party WebSocket - matches frontend path: /api/watchparty/{roomCode}/ws
        registry.addHandler(watchPartyWebSocketHandler, "/api/watchparty/*/ws")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
