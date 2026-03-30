package com.unikly.notificationservice.adapter.in.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceManager {

    private static final String PRESENCE_KEY_PREFIX = "user:";
    private static final String PRESENCE_KEY_SUFFIX = ":online";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redisTemplate;

    /** sessionId → userId for all currently connected WebSocket sessions. */
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public void register(String sessionId, String userId) {
        sessionUserMap.put(sessionId, userId);
        redisTemplate.opsForValue().set(presenceKey(userId), sessionId, PRESENCE_TTL);
        log.debug("WebSocket session registered: userId={}, sessionId={}", userId, sessionId);
    }

    public void unregister(String sessionId) {
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            redisTemplate.delete(presenceKey(userId));
            log.debug("WebSocket session unregistered: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey(userId)));
    }

    /**
     * Checks if a Redis key equals a specific value.
     * Used to detect if a user is active in a specific conversation.
     */
    public boolean isKeyEqual(String key, String expectedValue) {
        String value = redisTemplate.opsForValue().get(key);
        return expectedValue.equals(value);
    }

    /** Refresh TTL every 60s for all active connections to prevent stale expiry. */
    @Scheduled(fixedDelay = 60_000)
    public void refreshPresenceTtl() {
        sessionUserMap.forEach((sessionId, userId) ->
                redisTemplate.expire(presenceKey(userId), PRESENCE_TTL));
    }

    private String presenceKey(String userId) {
        return PRESENCE_KEY_PREFIX + userId + PRESENCE_KEY_SUFFIX;
    }
}
