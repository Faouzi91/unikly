package com.unikly.store.identity.persistence;

import com.unikly.store.identity.domain.StoreUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreUserRepository extends JpaRepository<StoreUser, Long> {
    Optional<StoreUser> findByEmail(String email);

    boolean existsByEmail(String email);
}