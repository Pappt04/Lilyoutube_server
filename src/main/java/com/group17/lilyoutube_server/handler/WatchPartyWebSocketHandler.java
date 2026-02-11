package com.group17.lilyoutube_server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group17.lilyoutube_server.dto.watchparty.VideoSyncMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WatchPartyWebSocketHandler extends TextWebSocketHandler {

    // Store sessions per room: Map<RoomCode, Set<Session>>
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomCode = extractRoomCode(session);
        if (roomCode != null) {
            roomSessions.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(session);
            String username = (String) session.getAttributes().getOrDefault("username", "Anonymous");
            System.out.println("User " + username + " connected to watch party: " + roomCode);

            // Notify other members that a new user joined
            VideoSyncMessage joinMessage = new VideoSyncMessage();
            joinMessage.setType("member_joined");
            joinMessage.setUsername(username);
            joinMessage.setMemberCount(roomSessions.get(roomCode).size());
            broadcastToRoom(roomCode, joinMessage, session);
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomCode = extractRoomCode(session);
        String username = (String) session.getAttributes().getOrDefault("username", "Anonymous");
        String payload = message.getPayload();

        try {
            VideoSyncMessage syncMessage = objectMapper.readValue(payload, VideoSyncMessage.class);

            // Validate message type
            if ("video_change".equals(syncMessage.getType())) {
                System.out.println("Video change in room " + roomCode + " to video " + syncMessage.getVideoId());

                // Broadcast to all members in the room
                broadcastToRoom(roomCode, syncMessage, null);
            }
        } catch (Exception e) {
            System.err.println("Error handling watch party message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomCode = extractRoomCode(session);
        if (roomCode != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomCode);
            if (sessions != null) {
                sessions.remove(session);

                String username = (String) session.getAttributes().getOrDefault("username", "Anonymous");
                System.out.println("User " + username + " disconnected from watch party: " + roomCode);

                // Notify other members that a user left
                VideoSyncMessage leaveMessage = new VideoSyncMessage();
                leaveMessage.setType("member_left");
                leaveMessage.setUsername(username);
                leaveMessage.setMemberCount(sessions.size());
                broadcastToRoom(roomCode, leaveMessage, null);

                if (sessions.isEmpty()) {
                    roomSessions.remove(roomCode);
                }
            }
        }
    }

    private void broadcastToRoom(String roomCode, VideoSyncMessage message, WebSocketSession excludeSession) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            Set<WebSocketSession> sessions = roomSessions.get(roomCode);

            if (sessions != null) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen() && !session.equals(excludeSession)) {
                        try {
                            session.sendMessage(new TextMessage(jsonMessage));
                        } catch (IOException e) {
                            System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting to room " + roomCode + ": " + e.getMessage());
        }
    }

    private String extractRoomCode(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null)
            return null;

        String path = uri.getPath();
        // Expected path: /api/watchparty/{roomCode}/ws
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("watchparty".equals(segments[i]) && i + 1 < segments.length) {
                return segments[i + 1];
            }
        }
        return null;
    }
}
