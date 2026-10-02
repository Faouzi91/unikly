package com.unikly.store.catalog.application;

import com.unikly.store.catalog.domain.CatalogProduct;
import com.unikly.store.catalog.persistence.CatalogProductRepository;
import com.unikly.store.identity.domain.StoreRole;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import com.unikly.store.reviews.persistence.ProductReviewRepository;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ProductReviewRepository reviews;

    public CatalogProductService(
            CatalogProductRepository products,
            StoreUserRepository users,
            ProductReviewRepository reviews) {
        this.products = products;
        this.users = users;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public List<ProductView> list() {
        Map<String, ProductRatingAggregate> aggregates = loadAggregates();
        return products.findAllByOrderByCreatedAtDesc().stream()
                .map(product -> view(product, aggregates))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> listMine(String email) {
        StoreUser seller = seller(email);
        Map<String, ProductRatingAggregate> aggregates = loadAggregates();
        return products.findAllBySellerIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(product -> view(product, aggregates))
                .toList();
    }

    public ProductView create(String email, ProductRequests.Upsert request) {
        StoreUser seller = seller(email);
        CatalogProduct product = new CatalogProduct(UUID.randomUUID().toString(), seller.getId(),
                clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(products.save(product), loadAggregates());
    }

    public ProductView update(String email, String id, ProductRequests.Upsert request) {
        StoreUser seller = seller(email);
        CatalogProduct product = ownedProduct(id, seller.getId());
        product.update(clean(request.name()), clean(request.description()), clean(request.category()),
                request.price(), image(request.image()), stock(request.stockQuantity()));
        return view(product, loadAggregates());
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

    private ProductView view(CatalogProduct product, Map<String, ProductRatingAggregate> aggregates) {
        ProductRatingAggregate aggregate = aggregates.get(product.getId());
        double rating = aggregate != null ? aggregate.rating() : 0.0;
        int reviewCount = aggregate != null ? aggregate.reviews() : 0;
        return new ProductView(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getImage(), rating, reviewCount,
                product.getSellerId(), product.getStockQuantity());
    }

    private Map<String, ProductRatingAggregate> loadAggregates() {
        Map<String, ProductRatingAggregate> map = new HashMap<>();
        for (Object[] row : reviews.findAggregateRatings()) {
            String productId = (String) row[0];
            Double avg = (Double) row[1];
            Long count = (Long) row[2];
            double rounded = java.math.BigDecimal.valueOf(avg != null ? avg : 0.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
            map.put(productId, new ProductRatingAggregate(rounded, count != null ? count.intValue() : 0));
        }
        return map;
    }

    private record ProductRatingAggregate(double rating, int reviews) {}

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
                              java.math.BigDecimal price, String image, double rating, int reviews, Long sellerId,
                              int stockQuantity) {}
}
