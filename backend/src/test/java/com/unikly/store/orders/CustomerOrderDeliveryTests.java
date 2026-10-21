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
class CustomerOrderDeliveryTests {
    @Autowired private CustomerOrderService orders;
    @Autowired private StoreUserRepository users;
    @Autowired private CatalogProductRepository products;

    @Test
    void standardDeliveryUnderThresholdCalculatesFiveDollarFee() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Mug", "Ceramic mug", "Kitchen",
                new BigDecimal("20.00"), "https://example.com/mug.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "STANDARD",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("STANDARD", order.deliveryMethod());
        assertEquals(new BigDecimal("5.00"), order.deliveryFee());
        assertEquals(new BigDecimal("25.00"), order.total());
    }

    @Test
    void standardDeliveryAtOrAboveThresholdIsFree() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Keyboard", "Mechanical", "Electronics",
                new BigDecimal("60.00"), "https://example.com/kb.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "STANDARD",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("STANDARD", order.deliveryMethod());
        assertEquals(new BigDecimal("0.00"), order.deliveryFee());
        assertEquals(new BigDecimal("60.00"), order.total());
    }

    @Test
    void expressDeliveryCalculatesFifteenDollarFee() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Headphones", "Wireless", "Electronics",
                new BigDecimal("100.00"), "https://example.com/hp.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "EXPRESS",
                List.of(new OrderRequests.Item(productId, 1)));

        CustomerOrderService.OrderView order = orders.create(buyerEmail, req);

        assertEquals("EXPRESS", order.deliveryMethod());
        assertEquals(new BigDecimal("15.00"), order.deliveryFee());
        assertEquals(new BigDecimal("115.00"), order.total());
    }

    @Test
    void unsupportedDeliveryMethodThrowsBadRequest() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("seller-" + suffix + "@example.com", "hash", "Seller", StoreRole.SELLER));
        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Shirt", "Cotton", "Apparel",
                new BigDecimal("30.00"), "https://example.com/shirt.jpg", 10));

        var req = new OrderRequests.Create("Buyer Name", buyerEmail, "+12025550123", "1 Market St",
                null, "SF", "CA", "94105", "US", "DRONE_AIRDROP",
                List.of(new OrderRequests.Item(productId, 1)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orders.create(buyerEmail, req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void sellerOrderViewOmitsBuyerEmailAndIncludesPhoneOnlyForExpressDelivery() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Buyer", StoreRole.BUYER));
        String sellerEmail = "seller-" + suffix + "@example.com";
        StoreUser seller = users.save(new StoreUser(sellerEmail, "hash", "Seller", StoreRole.SELLER));
        StoreUser otherSeller = users.save(new StoreUser("other-seller-" + suffix + "@example.com", "hash", "Other Seller", StoreRole.SELLER));

        String ownProductId = UUID.randomUUID().toString();
        String otherProductId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(ownProductId, seller.getId(), "Lamp", "Warm lamp", "Home",
                new BigDecimal("30.00"), "https://example.com/lamp.jpg", 10));
        products.save(new CatalogProduct(otherProductId, otherSeller.getId(), "Kettle", "Steel kettle", "Kitchen",
                new BigDecimal("40.00"), "https://example.com/kettle.jpg", 10));

        CustomerOrderService.OrderView standardOrder = orders.create(buyerEmail, new OrderRequests.Create(
                "Recipient Standard", buyerEmail, "+12025550111", "100 Main St",
                "Apt 4", "Austin", "TX", "78701", "US", "STANDARD",
                List.of(new OrderRequests.Item(ownProductId, 1), new OrderRequests.Item(otherProductId, 1))));

        CustomerOrderService.OrderView expressOrder = orders.create(buyerEmail, new OrderRequests.Create(
                "Recipient Express", buyerEmail, "+12025550222", "200 Oak Ave",
                null, "Seattle", "WA", "98101", "US", "EXPRESS",
                List.of(new OrderRequests.Item(ownProductId, 2))));

        List<CustomerOrderService.SellerOrderView> sellerOrders = orders.listForSeller(sellerEmail);
        assertEquals(2, sellerOrders.size());

        CustomerOrderService.SellerOrderView standardSellerView = sellerOrders.stream()
                .filter(o -> o.reference().equals(standardOrder.reference()))
                .findFirst()
                .orElseThrow();
        assertEquals("Recipient Standard", standardSellerView.fullName());
        org.junit.jupiter.api.Assertions.assertNull(standardSellerView.phone());
        assertEquals(1, standardSellerView.items().size());
        assertEquals(ownProductId, standardSellerView.items().getFirst().productId());

        CustomerOrderService.SellerOrderView expressSellerView = sellerOrders.stream()
                .filter(o -> o.reference().equals(expressOrder.reference()))
                .findFirst()
                .orElseThrow();
        assertEquals("Recipient Express", expressSellerView.fullName());
        assertEquals("+12025550222", expressSellerView.phone());
        assertEquals(1, expressSellerView.items().size());

        org.junit.jupiter.api.Assertions.assertFalse(
                java.util.Arrays.stream(CustomerOrderService.SellerOrderView.class.getRecordComponents())
                        .anyMatch(component -> "email".equals(component.getName())));
    }
}
