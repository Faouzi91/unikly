package com.unikly.store.catalog.application;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CatalogProductService {
    private static final String PLACEHOLDER = "/product-placeholder.svg";
    private final CatalogProductRepository products;
    private final StoreUserRepository users;

    public CatalogProductService(CatalogProductRepository products, StoreUserRepository users) {
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProductView> list() {
        return products.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> listMine(String email) {
        StoreUser seller = seller(email);
        return products.findAllBySellerIdOrderByCreatedAtDesc(seller.getId()).stream().map(this::view).toList();
    }

    public ProductView create(String email, ProductRequests.Upsert request) {
        StoreUser seller = seller(email);
        CatalogProduct product = new CatalogProduct(UUID.randomUUID().toString(), seller.getId(),
                clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(products.save(product));
    }

    public ProductView update(String email, String id, ProductRequests.Upsert request) {
        StoreUser seller = seller(email);
        CatalogProduct product = ownedProduct(id, seller.getId());
        product.update(clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(product);
    }

    public void delete(String email, String id) {
        StoreUser seller = seller(email);
        products.delete(ownedProduct(id, seller.getId()));
    }

    private StoreUser seller(String email) {
        StoreUser user = users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() != StoreRole.SELLER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private CatalogProduct ownedProduct(String id, Long sellerId) {
        return products.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ProductView view(CatalogProduct product) {
        return new ProductView(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getImage(), 0, 0, product.getSellerId(), product.getStockQuantity());
    }

    private static String clean(String value) { return value.trim(); }
    private static int stock(Integer value) { return value == null ? 10 : value; }

    private static String image(String value) {
        if (value == null || value.isBlank()) return PLACEHOLDER;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return uri.isAbsolute() && uri.getHost() != null &&
                    ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    ? uri.toString() : PLACEHOLDER;
        } catch (IllegalArgumentException exception) {
            return PLACEHOLDER;
        }
    }

    public record ProductView(String id, String name, String description, String category,
                              java.math.BigDecimal price, String image, int rating, int reviews, Long sellerId,
                              int stockQuantity) {}
}
