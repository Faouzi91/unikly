package com.unikly.store.profile.persistence;

import com.unikly.store.profile.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
}