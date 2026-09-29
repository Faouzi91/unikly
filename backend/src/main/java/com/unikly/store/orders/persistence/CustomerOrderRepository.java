package com.unikly.store.orders.persistence;

import com.unikly.store.orders.domain.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {
    @EntityGraph(attributePaths = "items")
    List<CustomerOrder> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
