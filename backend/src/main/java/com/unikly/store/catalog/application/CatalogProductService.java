package com.unikly.store.catalog.application;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.application.IdentityAccountService.AccountIdentity;
import com.unikly.store.identity.application.IdentityAccountService.AccountRole;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final IdentityAccountService identity;

    public CatalogProductService(
            CatalogProductRepository products,
            IdentityAccountService identity) {
        this.products = products;
        this.identity = identity;
    }

    @Transactional(readOnly = true)
    public List<ProductView> list() {
        return products.findAllByOrderByCreatedAtDesc().stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> listMine(String email) {
        AccountIdentity seller = seller(email);
        return products.findAllBySellerIdOrderByCreatedAtDesc(seller.id()).stream()
                .map(this::view)
                .toList();
    }

    public ProductView create(String email, ProductRequests.Upsert request) {
        AccountIdentity seller = seller(email);
        CatalogProduct product = new CatalogProduct(UUID.randomUUID().toString(), seller.id(),
                clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(products.save(product));
    }

    public ProductView update(String email, String id, ProductRequests.Upsert request) {
        AccountIdentity seller = seller(email);
        CatalogProduct product = ownedProduct(id, seller.id());
        product.update(clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(product);
    }

    public void delete(String email, String id) {
        AccountIdentity seller = seller(email);
        products.delete(ownedProduct(id, seller.id()));
    }

    @Transactional(readOnly = true)
    public Optional<ProductSnapshot> findProduct(String id) {
        return products.findById(id).map(this::snapshot);
    }

    @Transactional(readOnly = true)
    public ProductSnapshot requireProduct(String id) {
        return findProduct(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional(readOnly = true)
    public Map<String, ProductSnapshot> findProducts(Collection<String> ids) {
        return products.findAllById(ids).stream()
                .map(this::snapshot)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(ProductSnapshot::id, product -> product));
    }

    @Transactional(readOnly = true)
    public boolean exists(String id) {
        return products.existsById(id);
    }

    public ProductSnapshot reserveStock(String id, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation quantity must be positive");
        }
        CatalogProduct product = products.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A product is no longer available"));
        if (quantity > product.getStockQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    product.getName() + " has only " + product.getStockQuantity() + " left in stock");
        }
        product.decreaseStock(quantity);
        return snapshot(product);
    }

    public void restockIfPresent(String id, int quantity) {
        products.findByIdForUpdate(id).ifPresent(product -> product.increaseStock(quantity));
    }

    private AccountIdentity seller(String email) {
        AccountIdentity user = identity.requireByEmail(email);
        if (user.role() != AccountRole.SELLER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private CatalogProduct ownedProduct(String id, Long sellerId) {
        return products.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ProductView view(CatalogProduct product) {
        return new ProductView(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getImage(),
                product.getSellerId(), product.getStockQuantity(), product.isDemo());
    }

    private ProductSnapshot snapshot(CatalogProduct product) {
        return new ProductSnapshot(product.getId(), product.getName(), product.getCategory(), product.getPrice(),
                product.getImage(), product.getSellerId(), product.getStockQuantity());
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
                              java.math.BigDecimal price, String image, Long sellerId, int stockQuantity,
                              boolean isDemo) {}

    public record ProductSnapshot(String id, String name, String category, java.math.BigDecimal price, String image,
                                  Long sellerId, int stockQuantity) {}
}
