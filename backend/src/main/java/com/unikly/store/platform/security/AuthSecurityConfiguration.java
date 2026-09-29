package com.unikly.store.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.unikly.store.identity.domain.StorePermission;
import com.unikly.store.identity.security.StoreUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableMethodSecurity
public class AuthSecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository contextRepository,
            StoreUserDetailsService userDetailsService,
            SessionRegistry sessionRegistry) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .securityContext(context -> context
                        .securityContextRepository(contextRepository)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                    .maximumSessions(5)
                    .sessionRegistry(sessionRegistry))
                .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login", "/actuator/health").permitAll()
                    .requestMatchers("/api/auth/me").hasAuthority(StorePermission.ACCOUNT_READ_SELF.authority())
                    .requestMatchers(HttpMethod.POST, "/api/auth/password").hasAuthority(StorePermission.ACCOUNT_UPDATE_SELF.authority())
                    .requestMatchers(HttpMethod.GET, "/api/profile/me").hasAuthority(StorePermission.ACCOUNT_READ_SELF.authority())
                    .requestMatchers(HttpMethod.POST, "/api/orders").hasAuthority(StorePermission.ORDER_CREATE_SELF.authority())
                    .requestMatchers(HttpMethod.GET, "/api/orders/mine").hasAuthority(StorePermission.ORDER_READ_SELF.authority())
                    .requestMatchers(HttpMethod.GET, "/api/orders/seller").hasAuthority(StorePermission.ORDER_FULFILL_OWN.authority())
                    .requestMatchers(HttpMethod.PUT, "/api/orders/seller/**").hasAuthority(StorePermission.ORDER_FULFILL_OWN.authority())
                    .requestMatchers(HttpMethod.PUT, "/api/profile/me").hasAuthority(StorePermission.ACCOUNT_UPDATE_SELF.authority())
                    .requestMatchers(HttpMethod.GET, "/api/products/mine").hasRole("SELLER")
                    .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("SELLER")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("SELLER")
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                    .requestMatchers("/api/admin/access-check").hasAuthority(StorePermission.PLATFORM_REPORT_READ.authority())
                    .requestMatchers("/api/admin/**").hasAuthority(StorePermission.PLATFORM_ADMIN.authority())
                    .requestMatchers("/api/**").denyAll()
                        .anyRequest().permitAll())
                    .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("UNIKLY_SESSION")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));

        return http.build();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, java.util.function.Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            return request.getHeader(csrfToken.getHeaderName()) != null
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}