package com.unikly.store.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unikly.store.cart.application.CartRequests;
import com.unikly.store.cart.application.CartViews.CartView;
import com.unikly.store.cart.application.CustomerCartService;
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
class CustomerCartTests {

    @Autowired private CustomerCartService cartService;
    @Autowired private CustomerOrderService orderService;
    @Autowired private StoreUserRepository users;
    @Autowired private CatalogProductRepository products;

    @Test
    void buyerCanAddItemsToCartAndRetrieveCart() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "cart-buyer-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Cart Buyer", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("cart-seller-" + suffix + "@example.com", "hash", "Cart Seller", StoreRole.SELLER));

        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Ceramic Mug", "Handmade mug", "Kitchen",
                new BigDecimal("15.50"), "https://example.com/mug.jpg", 10));

        CartView view = cartService.addItem(buyerEmail, new CartRequests.AddItem(productId, 2));

        assertNotNull(view);
        assertEquals(1, view.items().size());
        assertEquals(2, view.itemCount());
        assertEquals(new BigDecimal("31.00"), view.subtotal());
        assertEquals("Ceramic Mug", view.items().getFirst().productName());

        CartView fetched = cartService.getCart(buyerEmail);
        assertEquals(1, fetched.items().size());
        assertEquals(2, fetched.itemCount());
        assertEquals(new BigDecimal("31.00"), fetched.subtotal());
    }

    @Test
    void buyerCannotAddMoreThanAvailableStock() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "cart-buyer2-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Cart Buyer 2", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("cart-seller2-" + suffix + "@example.com", "hash", "Cart Seller 2", StoreRole.SELLER));

        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Limited Notebook", "Leather notebook", "Stationery",
                new BigDecimal("20.00"), "https://example.com/book.jpg", 3));

        cartService.addItem(buyerEmail, new CartRequests.AddItem(productId, 2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addItem(buyerEmail, new CartRequests.AddItem(productId, 2)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void buyerCanUpdateItemQuantityAndRemoveItem() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "cart-buyer3-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Cart Buyer 3", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("cart-seller3-" + suffix + "@example.com", "hash", "Cart Seller 3", StoreRole.SELLER));

        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Steel Flask", "Insulated flask", "Outdoor",
                new BigDecimal("25.00"), "https://example.com/flask.jpg", 8));

        cartService.addItem(buyerEmail, new CartRequests.AddItem(productId, 1));
        CartView updated = cartService.updateItem(buyerEmail, productId, new CartRequests.UpdateItem(4));
        assertEquals(4, updated.itemCount());
        assertEquals(new BigDecimal("100.00"), updated.subtotal());

        CartView afterRemoval = cartService.removeItem(buyerEmail, productId);
        assertTrue(afterRemoval.items().isEmpty());
        assertEquals(0, afterRemoval.itemCount());
    }

    @Test
    void guestCartMergedUponLoginCappedAtStock() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "cart-buyer4-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Cart Buyer 4", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("cart-seller4-" + suffix + "@example.com", "hash", "Cart Seller 4", StoreRole.SELLER));

        String productA = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productA, seller.getId(), "Pen A", "Ballpoint", "Stationery",
                new BigDecimal("5.00"), "https://example.com/pen.jpg", 2));
        String productB = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productB, seller.getId(), "Pen B", "Gel pen", "Stationery",
                new BigDecimal("7.00"), "https://example.com/penb.jpg", 10));

        // Guest wants 5 of product A (stock is 2), and 3 of product B (stock is 10)
        CartRequests.MergeRequest mergeReq = new CartRequests.MergeRequest(List.of(
                new CartRequests.MergeItem(productA, 5),
                new CartRequests.MergeItem(productB, 3)
        ));

        CartView merged = cartService.mergeCart(buyerEmail, mergeReq);
        assertEquals(2, merged.items().size());
        assertEquals(5, merged.itemCount()); // 2 of A + 3 of B = 5
        assertEquals(new BigDecimal("31.00"), merged.subtotal()); // 2 * 5.00 + 3 * 7.00 = 10.00 + 21.00 = 31.00
    }

    @Test
    void orderPlacementAutomaticallyClearsBuyerCart() {
        String suffix = UUID.randomUUID().toString();
        String buyerEmail = "cart-buyer5-" + suffix + "@example.com";
        users.save(new StoreUser(buyerEmail, "hash", "Cart Buyer 5", StoreRole.BUYER));
        StoreUser seller = users.save(new StoreUser("cart-seller5-" + suffix + "@example.com", "hash", "Cart Seller 5", StoreRole.SELLER));

        String productId = UUID.randomUUID().toString();
        products.save(new CatalogProduct(productId, seller.getId(), "Desk Mat", "Wool felt desk mat", "Home",
                new BigDecimal("40.00"), "https://example.com/mat.jpg", 5));

        cartService.addItem(buyerEmail, new CartRequests.AddItem(productId, 2));
        assertEquals(1, cartService.getCart(buyerEmail).items().size());

        OrderRequests.Create orderReq = new OrderRequests.Create(
                "Buyer Five", buyerEmail, "+12025550199", "123 Main St",
                null, "Portland", "OR", "97201", "United States",
                List.of(new OrderRequests.Item(productId, 2))
        );
        orderService.create(buyerEmail, orderReq);

        CartView cartAfterOrder = cartService.getCart(buyerEmail);
        assertTrue(cartAfterOrder.items().isEmpty());
        assertEquals(0, cartAfterOrder.itemCount());
    }
}
