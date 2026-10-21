package com.unikly.store.catalog.application;

import com.unikly.store.catalog.domain.SellerStoreProfile;
import com.unikly.store.catalog.persistence.SellerStoreProfileRepository;
import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.application.IdentityAccountService.AccountIdentity;
import com.unikly.store.identity.application.IdentityAccountService.AccountRole;
import com.unikly.store.identity.application.IdentityAccountService.PublicSellerIdentity;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SellerStoreProfileService {
    private final SellerStoreProfileRepository profiles;
    private final IdentityAccountService identity;

    public SellerStoreProfileService(
            SellerStoreProfileRepository profiles,
            IdentityAccountService identity) {
        this.profiles = profiles;
        this.identity = identity;
    }

    @Transactional(readOnly = true)
    public SellerProfileView getPublicProfile(Long sellerId) {
        PublicSellerIdentity seller = identity.findSellerIdentityById(sellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller store profile not found"));
        return profiles.findById(sellerId)
                .map(this::toView)
                .orElseGet(() -> defaultView(seller));
    }

    @Transactional(readOnly = true)
    public SellerProfileView getMyProfile(String email) {
        AccountIdentity seller = requireSeller(email);
        return profiles.findById(seller.id())
                .map(this::toView)
                .orElseGet(() -> new SellerProfileView(
                        seller.id(), seller.displayName(), null, null, null, null));
    }

    public SellerProfileView updateMyProfile(String email, SellerStoreProfileRequests.Upsert request) {
        AccountIdentity seller = requireSeller(email);
        String storeName = request.storeName().trim();
        String headline = clean(request.headline());
        String description = clean(request.description());
        String location = clean(request.location());
        String supportEmail = cleanEmail(request.supportEmail());

        SellerStoreProfile profile = profiles.findById(seller.id())
                .map(existing -> {
                    existing.update(storeName, headline, description, location, supportEmail);
                    return existing;
                })
                .orElseGet(() -> new SellerStoreProfile(
                        seller.id(), storeName, headline, description, location, supportEmail));
        return toView(profiles.save(profile));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> resolveStoreNames(Collection<Long> sellerIds) {
        Set<Long> nonNullIds = sellerIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (nonNullIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PublicSellerIdentity> sellers = identity.findSellerIdentitiesByIds(nonNullIds);
        Map<Long, String> names = new HashMap<>();
        sellers.forEach((id, seller) -> names.put(id, seller.displayName()));
        for (SellerStoreProfile profile : profiles.findAllById(nonNullIds)) {
            if (sellers.containsKey(profile.getSellerId())) {
                names.put(profile.getSellerId(), profile.getStoreName());
            }
        }
        return Map.copyOf(names);
    }

    private AccountIdentity requireSeller(String email) {
        AccountIdentity user = identity.requireByEmail(email);
        if (user.role() != AccountRole.SELLER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private SellerProfileView toView(SellerStoreProfile profile) {
        return new SellerProfileView(
                profile.getSellerId(),
                profile.getStoreName(),
                profile.getHeadline(),
                profile.getDescription(),
                profile.getLocation(),
                profile.getSupportEmail());
    }

    private SellerProfileView defaultView(PublicSellerIdentity seller) {
        return new SellerProfileView(seller.id(), seller.displayName(), null, null, null, null);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String cleanEmail(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    public record SellerProfileView(
            Long sellerId,
            String storeName,
            String headline,
            String description,
            String location,
            String supportEmail) {}
}
