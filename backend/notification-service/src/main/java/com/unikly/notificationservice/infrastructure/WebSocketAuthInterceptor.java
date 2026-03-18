package com.unikly.notificationservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final WebSocketPresenceManager presenceManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token != null) {
                try {
                    var jwt = jwtDecoder.decode(token);
                    String userId = jwt.getSubject();
                    accessor.setUser(() -> userId);
                    presenceManager.register(accessor.getSessionId(), userId);
                    log.info("WebSocket CONNECT authenticated: userId={}", userId);
                } catch (Exception e) {
                    log.warn("WebSocket JWT validation failed: {}", e.getMessage());
                    // Allow connection but without authenticated principal
                }
            }
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            presenceManager.unregister(accessor.getSessionId());
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first (Bearer <token>)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Fall back to "token" header (query param forwarded by SockJS)
        return accessor.getFirstNativeHeader("token");
    }
}
