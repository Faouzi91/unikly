package com.unikly.store.identity.application;

import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityAccountServiceImpl implements IdentityAccountService {
    private final StoreUserRepository users;
    private final PasswordEncoder passwords;

    public IdentityAccountServiceImpl(StoreUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountIdentity requireByEmail(String email) {
        StoreUser user = requireUserByEmail(email);
        return new AccountIdentity(user.getId(), user.getEmail(), user.getDisplayName(),
                AccountRole.valueOf(user.getRole().name()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountIdentity> findByEmail(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(user -> new AccountIdentity(user.getId(), user.getEmail(), user.getDisplayName(),
                        AccountRole.valueOf(user.getRole().name())));
    }

    @Override
    @Transactional
    public void updateDisplayName(String email, String displayName) {
        StoreUser user = requireUserByEmail(email);
        user.updateDisplayName(displayName.trim());
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        StoreUser user = requireUserByEmail(email);
        if (!passwords.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwords.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a password you have not used before");
        }
        user.updatePasswordHash(passwords.encode(newPassword));
    }

    private StoreUser requireUserByEmail(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
