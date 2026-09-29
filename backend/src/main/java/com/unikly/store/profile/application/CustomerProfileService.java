package com.unikly.store.profile.application;

import com.unikly.store.identity.application.IdentityAccountService;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.profile.domain.CustomerProfile;
import com.unikly.store.profile.domain.CustomerProfile.ContactDetails;
import com.unikly.store.profile.persistence.CustomerProfileRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {
    private final IdentityAccountService identity;
    private final CustomerProfileRepository profiles;

    public CustomerProfileService(IdentityAccountService identity, CustomerProfileRepository profiles) {
        this.identity = identity;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileView get(String email) {
        StoreUser user = identity.requireByEmail(email);
        CustomerProfile profile = profiles.findById(user.getId()).orElseGet(() -> new CustomerProfile(user.getId()));
        return toView(user, profile);
    }

    @Transactional
    public ProfileView update(String email, ProfileRequests.Update request) {
        StoreUser user = identity.requireByEmail(email);
        identity.updateDisplayName(email, request.displayName());
        CustomerProfile profile = profiles.findById(user.getId()).orElseGet(() -> new CustomerProfile(user.getId()));
        profile.update(new ContactDetails(
                clean(request.phoneNumber()), clean(request.addressLine1()), clean(request.addressLine2()),
                clean(request.city()), clean(request.region()), clean(request.postalCode()),
                clean(request.countryCode()) == null ? null : clean(request.countryCode()).toUpperCase(Locale.ROOT)));
        profiles.save(profile);
        return toView(identity.requireByEmail(email), profile);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ProfileView toView(StoreUser user, CustomerProfile profile) {
        return new ProfileView(user.getId(), user.getEmail(), user.getDisplayName(),
                profile.getPhoneNumber(), profile.getAddressLine1(), profile.getAddressLine2(),
                profile.getCity(), profile.getRegion(), profile.getPostalCode(), profile.getCountryCode());
    }

    public record ProfileView(
            Long id,
            String email,
            String displayName,
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String region,
            String postalCode,
            String countryCode) {
    }
}