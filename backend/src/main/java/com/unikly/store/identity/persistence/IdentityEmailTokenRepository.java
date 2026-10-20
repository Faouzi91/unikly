package com.unikly.store.identity.persistence;

import com.unikly.store.identity.domain.IdentityEmailPurpose;
import com.unikly.store.identity.domain.IdentityEmailToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityEmailTokenRepository extends JpaRepository<IdentityEmailToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from IdentityEmailToken token where token.tokenHash = :tokenHash and token.purpose = :purpose")
    Optional<IdentityEmailToken> findForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") IdentityEmailPurpose purpose);

    @Modifying
    @Query("delete from IdentityEmailToken token where token.userId = :userId and token.purpose = :purpose")
    void deleteByUserAndPurpose(@Param("userId") Long userId, @Param("purpose") IdentityEmailPurpose purpose);

    @Modifying
    @Query("delete from IdentityEmailToken token where token.expiresAt < :now or token.consumedAt < :cutoff")
    int deleteExpiredOrConsumed(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
