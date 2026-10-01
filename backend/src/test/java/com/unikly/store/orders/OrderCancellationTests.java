package com.unikly.store.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.orders.application.CustomerOrderService;
import com.unikly.store.orders.application.OrderRequests;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class OrderCancellationTests {
    @Autowired private CustomerOrderService orders;
    @Autowired private StoreUserRepository users;
    @Autowired private CatalogProductRepository products;

    @Test
    void buyerCanCancelOwnPlacedOrderAndStockIsRestored() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        StoreUser buyer = users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Desk lamp", "Warm light", "Home",
                new BigDecimal("25.00"), "https://example.com/lamp.jpg", 4));

        CustomerOrderService.OrderView created = orders.create(buyerEmail, request(productId, 2));
        assertEquals(2, products.findById(productId).orElseThrow().getStockQuantity());

        CustomerOrderService.OrderView canceled = orders.cancel(buyerEmail, created.reference());

        assertEquals("CANCELED", canceled.status());
        assertEquals("CANCELED", canceled.items().getFirst().status());
        assertEquals(4, products.findById(productId).orElseThrow().getStockQuantity());
    }

    @Test
    void buyerCannotCancelAnotherBuyersOrder() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        StoreUser buyer = users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser otherBuyer = users.save(new StoreUser("other-" + suffix + "@example.com", "hash", "Other buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Desk lamp", "Warm light", "Home",
                new BigDecimal("25.00"), "https://example.com/lamp.jpg", 4));

        CustomerOrderService.OrderView created = orders.create(buyerEmail, request(productId, 1));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orders.cancel(otherBuyer.getEmail(), created.reference()));
        assertEquals(404, exception.getStatusCode().value());
    }

    private static OrderRequests.Create request(String productId, int quantity) {
        return new OrderRequests.Create("Buyer Name", "buyer@example.com", "+12025550123", "1 Market Street",
                null, "San Francisco", "CA", "94105", "United States",
                List.of(new OrderRequests.Item(productId, quantity)));
    }
}
