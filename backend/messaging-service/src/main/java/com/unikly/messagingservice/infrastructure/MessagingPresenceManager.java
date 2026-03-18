package com.unikly.messagingservice.infrastructure;

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
public class MessagingPresenceManager {

    private static final String PRESENCE_KEY_PREFIX = "msg-user:";
    private static final String PRESENCE_KEY_SUFFIX = ":online";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redisTemplate;

    /** sessionId → userId for all connected messaging WebSocket sessions. */
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public void register(String sessionId, String userId) {
        sessionUserMap.put(sessionId, userId);
        redisTemplate.opsForValue().set(presenceKey(userId), sessionId, PRESENCE_TTL);
        log.debug("Messaging WS connected: userId={}, sessionId={}", userId, sessionId);
    }

    public void unregister(String sessionId) {
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            redisTemplate.delete(presenceKey(userId));
            log.debug("Messaging WS disconnected: userId={}, sessionId={}", userId, sessionId);
        }
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey(userId)));
    }

    @Scheduled(fixedDelay = 60_000)
    public void refreshPresenceTtl() {
        sessionUserMap.forEach((sessionId, userId) ->
                redisTemplate.expire(presenceKey(userId), PRESENCE_TTL));
    }

    private String presenceKey(String userId) {
        return PRESENCE_KEY_PREFIX + userId + PRESENCE_KEY_SUFFIX;
    }
}
