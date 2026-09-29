package com.unikly.store.orders.persistence;

import com.unikly.store.orders.domain.CustomerOrder;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {
    @EntityGraph(attributePaths = "items")
    List<CustomerOrder> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct o from CustomerOrder o join o.items item where item.sellerId = :sellerId order by o.createdAt desc")
    List<CustomerOrder> findAllForSeller(@Param("sellerId") Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findByReference(String reference);
}
