package com.unikly.store.catalog.api;

import com.unikly.store.catalog.application.CatalogProductService;
import com.unikly.store.catalog.application.ProductRequests;
import com.unikly.store.catalog.application.CatalogProductService.ProductView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/products")
public class CatalogProductController {
    private final CatalogProductService catalog;

    public CatalogProductController(CatalogProductService catalog) { this.catalog = catalog; }

    @GetMapping
    public List<ProductView> list() { return catalog.list(); }

    @GetMapping("/mine")
    public List<ProductView> listMine(Authentication authentication) { return catalog.listMine(authentication.getName()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(Authentication authentication, @Valid @RequestBody ProductRequests.Upsert request) {
        return catalog.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public ProductView update(Authentication authentication, @PathVariable String id,
                               @Valid @RequestBody ProductRequests.Upsert request) {
        return catalog.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable String id) {
        catalog.delete(authentication.getName(), id);
    }
}
