package com.unikly.store.identity.application;

import com.unikly.store.identity.persistence.IdentityEmailTokenRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdentityEmailTokenCleanup {
    private final IdentityEmailTokenRepository tokens;
    private final Clock clock;

    public IdentityEmailTokenCleanup(IdentityEmailTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void removeExpiredTokens() {
        var now = clock.instant();
        tokens.deleteExpiredOrConsumed(now, now.minus(Duration.ofDays(2)));
    }
}
