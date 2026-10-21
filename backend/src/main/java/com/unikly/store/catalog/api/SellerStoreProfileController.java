package com.unikly.store.catalog.api;

import com.unikly.store.catalog.application.SellerStoreProfileRequests;
import com.unikly.store.catalog.application.SellerStoreProfileService;
import com.unikly.store.catalog.application.SellerStoreProfileService.SellerProfileView;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers")
public class SellerStoreProfileController {
    private final SellerStoreProfileService storeProfiles;

    public SellerStoreProfileController(SellerStoreProfileService storeProfiles) {
        this.storeProfiles = storeProfiles;
    }

    @GetMapping("/{sellerId}/profile")
    public SellerProfileView getPublicProfile(@PathVariable Long sellerId) {
        return storeProfiles.getPublicProfile(sellerId);
    }

    @GetMapping("/me/profile")
    public SellerProfileView getMyProfile(Authentication authentication) {
        return storeProfiles.getMyProfile(authentication.getName());
    }

    @PutMapping("/me/profile")
    public SellerProfileView updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody SellerStoreProfileRequests.Upsert request) {
        return storeProfiles.updateMyProfile(authentication.getName(), request);
    }
}
