package com.unikly.store.catalog.persistence;

import com.unikly.store.catalog.domain.CatalogProduct;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, String> {
    List<CatalogProduct> findAllByOrderByCreatedAtDesc();
    List<CatalogProduct> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
    Optional<CatalogProduct> findByIdAndSellerId(String id, Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from CatalogProduct product where product.id = :id")
    Optional<CatalogProduct> findByIdForUpdate(@Param("id") String id);
}
