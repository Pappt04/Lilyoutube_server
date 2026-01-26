package com.group17.lilyoutube_server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // store sessions per video: Map<VideoName, Set<Session>>
    private final Map<String, Set<WebSocketSession>> videoSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String videoName = extractVideoName(session);
        if (videoName != null) {
            videoSessions.computeIfAbsent(videoName, k -> ConcurrentHashMap.newKeySet()).add(session);
            System.out.println("New chat connection for video: " + videoName);
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String videoName = extractVideoName(session);
        String sender = (String) session.getAttributes().getOrDefault("username", "Anonymous");
        String payload = message.getPayload();

        try {
            Map<String, String> incoming = objectMapper.readValue(payload, Map.class);
            String content = incoming.get("content");

            if (content != null && !content.trim().isEmpty()) {
                Map<String, String> outgoing = Map.of(
                        "sender", sender,
                        "content", content);
                String jsonMessage = objectMapper.writeValueAsString(outgoing);

                Set<WebSocketSession> sessions = videoSessions.getOrDefault(videoName, Collections.emptySet());
                for (WebSocketSession s : sessions) {
                    if (s.isOpen()) {
                        try {
                            s.sendMessage(new TextMessage(jsonMessage));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling info: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String videoName = extractVideoName(session);
        if (videoName != null) {
            Set<WebSocketSession> sessions = videoSessions.get(videoName);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    videoSessions.remove(videoName);
                }
            }
        }
    }

    private String extractVideoName(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null)
            return null;

        String path = uri.getPath();
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("stream".equals(segments[i]) && i + 1 < segments.length) {
                return segments[i + 1];
            }
        }
        return null;
    }
}
