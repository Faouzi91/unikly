package com.unikly.store.orders.api;

import com.unikly.store.orders.application.CustomerOrderService;
import com.unikly.store.orders.application.CustomerOrderService.OrderView;
import com.unikly.store.orders.application.CustomerOrderService.SellerOrderView;
import com.unikly.store.orders.application.OrderRequests;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class CustomerOrderController {
    private final CustomerOrderService orders;
    public CustomerOrderController(CustomerOrderService orders) { this.orders = orders; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(Authentication authentication, @Valid @RequestBody OrderRequests.Create request) {
        return orders.create(authentication.getName(), request);
    }

    @GetMapping("/mine")
    public List<OrderView> listMine(Authentication authentication) {
        return orders.listMine(authentication.getName());
    }

    @PutMapping("/mine/{reference}/confirm-delivery")
    public OrderView confirmDelivery(Authentication authentication, @PathVariable String reference) {
        return orders.confirmDelivery(authentication.getName(), reference);
    }

    @GetMapping("/seller")
    public List<SellerOrderView> listForSeller(Authentication authentication) {
        return orders.listForSeller(authentication.getName());
    }

    @PutMapping("/seller/{reference}/status")
    public SellerOrderView updateSellerStatus(
            Authentication authentication,
            @PathVariable String reference,
            @Valid @RequestBody OrderRequests.UpdateFulfillmentStatus request) {
        return orders.updateSellerFulfillment(authentication.getName(), reference, request);
    }
}
