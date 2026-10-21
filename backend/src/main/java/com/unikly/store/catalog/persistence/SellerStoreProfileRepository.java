package com.unikly.store.catalog.persistence;

import com.unikly.store.catalog.domain.SellerStoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerStoreProfileRepository extends JpaRepository<SellerStoreProfile, Long> {}
