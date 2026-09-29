package com.unikly.store.identity.security;

import com.unikly.store.identity.domain.StorePermission;
import com.unikly.store.identity.domain.StoreUser;
import com.unikly.store.identity.persistence.StoreUserRepository;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StoreUserDetailsService implements UserDetailsService {
    private final StoreUserRepository users;

    public StoreUserDetailsService(StoreUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        StoreUser user = users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        user.getRole().permissions().stream()
            .map(StorePermission::authority)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
            .authorities(authorities)
                .build();
    }
}