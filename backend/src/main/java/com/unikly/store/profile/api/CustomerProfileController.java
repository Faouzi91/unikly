package com.unikly.store.profile.api;

import com.unikly.store.profile.application.CustomerProfileService;
import com.unikly.store.profile.application.ProfileRequests;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/me")
public class CustomerProfileController {
    private final CustomerProfileService profiles;

    public CustomerProfileController(CustomerProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public CustomerProfileService.ProfileView get(Authentication authentication) {
        return profiles.get(authentication.getName());
    }

    @PutMapping
    public CustomerProfileService.ProfileView update(
            Authentication authentication,
            @Valid @RequestBody ProfileRequests.Update request) {
        return profiles.update(authentication.getName(), request);
    }
}