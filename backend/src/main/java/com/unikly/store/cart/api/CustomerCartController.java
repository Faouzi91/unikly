package com.unikly.store.cart.api;

import com.unikly.store.cart.application.CartRequests;
import com.unikly.store.cart.application.CartViews.CartView;
import com.unikly.store.cart.application.CustomerCartService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CustomerCartController {

    private final CustomerCartService cartService;

    public CustomerCartController(CustomerCartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartView getCart(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    public CartView addItem(Principal principal, @Valid @RequestBody CartRequests.AddItem request) {
        return cartService.addItem(principal.getName(), request);
    }

    @PutMapping("/items/{productId}")
    public CartView updateItem(
            Principal principal,
            @PathVariable String productId,
            @Valid @RequestBody CartRequests.UpdateItem request) {
        return cartService.updateItem(principal.getName(), productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartView removeItem(Principal principal, @PathVariable String productId) {
        return cartService.removeItem(principal.getName(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Principal principal) {
        cartService.clearCart(principal.getName());
    }

    @PostMapping("/merge")
    public CartView mergeCart(Principal principal, @Valid @RequestBody CartRequests.MergeRequest request) {
        return cartService.mergeCart(principal.getName(), request);
    }
}
