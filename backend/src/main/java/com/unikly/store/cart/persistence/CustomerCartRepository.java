package com.unikly.store.cart.persistence;

import com.unikly.store.cart.domain.CustomerCart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCartRepository extends JpaRepository<CustomerCart, String> {

    @EntityGraph(attributePaths = "items")
    Optional<CustomerCart> findByBuyerId(Long buyerId);

    void deleteByBuyerId(Long buyerId);
}
