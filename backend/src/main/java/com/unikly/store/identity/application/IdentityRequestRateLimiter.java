package com.unikly.store.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class IdentityRequestRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_CLIENT_REQUESTS = 30;
    private static final int MAX_ADDRESS_REQUESTS = 5;
    private static final int MAX_BUCKETS = 20_000;
    private static final int CLEANUP_EVERY = 64;

    private final Clock clock;
    private final Map<String, Bucket> buckets = new LinkedHashMap<>(256, 0.75f, true);
    private int operations;

    public IdentityRequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean allow(String action, String clientAddress, String email) {
        Instant now = clock.instant();
        String clientKey = action + ":client:" + hash(clientAddress == null ? "unknown" : clientAddress);
        if (!consume(clientKey, MAX_CLIENT_REQUESTS, now)) return false;

        String addressKey = action + ":address:" + hash(email.trim().toLowerCase(java.util.Locale.ROOT));
        return consume(addressKey, MAX_ADDRESS_REQUESTS, now);
    }

    private boolean consume(String key, int limit, Instant now) {
        if (++operations % CLEANUP_EVERY == 0 || buckets.size() >= MAX_BUCKETS - CLEANUP_EVERY) {
            removeExpired(now);
        }

        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= MAX_BUCKETS) return false;
            buckets.put(key, new Bucket(now, 1));
            return true;
        }
        if (!bucket.startedAt.plus(WINDOW).isAfter(now)) {
            bucket.startedAt = now;
            bucket.count = 1;
            return true;
        }
        if (bucket.count >= limit) return false;
        bucket.count++;
        return true;
    }

    private void removeExpired(Instant now) {
        Iterator<Bucket> iterator = buckets.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().startedAt.plus(WINDOW).isAfter(now)) iterator.remove();
        }
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class Bucket {
        private Instant startedAt;
        private int count;

        private Bucket(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
