package com.unikly.userservice.application.service;

import com.unikly.userservice.adapter.in.web.dto.LoginRequest;
import com.unikly.userservice.adapter.in.web.dto.RefreshTokenRequest;
import com.unikly.userservice.adapter.in.web.dto.SocialAuthorizationRequest;
import com.unikly.userservice.adapter.in.web.dto.SocialCodeExchangeRequest;
import com.unikly.userservice.config.KeycloakAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RestClient keycloakRestClient;
    private final KeycloakAuthProperties keycloakAuthProperties;

    public Map<String, Object> login(LoginRequest request) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "password");
        form.add("username", request.username());
        form.add("password", request.password());
        return fetchToken(form, "Invalid credentials");
    }

    public Map<String, Object> refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", request.refreshToken());
        return fetchToken(form, "Session expired. Please sign in again.");
    }

    public String buildSocialAuthorizationUrl(SocialAuthorizationRequest request) {
        String provider = normalizeProviderAlias(request.provider());
        String authorizationUrl = "%s/realms/%s/protocol/openid-connect/auth"
                .formatted(keycloakAuthProperties.url(), keycloakAuthProperties.realm());

        return UriComponentsBuilder
                .fromUriString(authorizationUrl)
                .queryParam("client_id", keycloakAuthProperties.clientId())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("redirect_uri", request.redirectUri())
                .queryParam("state", request.state())
                .queryParam("kc_idp_hint", provider)
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();
    }

    public Map<String, Object> exchangeSocialCode(SocialCodeExchangeRequest request) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "authorization_code");
        form.add("code", request.code());
        form.add("redirect_uri", request.redirectUri());
        form.add("code_verifier", request.codeVerifier());
        return fetchToken(form, "Social sign-in failed. Please try again.");
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakAuthProperties.clientId());
        if (StringUtils.hasText(keycloakAuthProperties.clientSecret())) {
            form.add("client_secret", keycloakAuthProperties.clientSecret());
        }
        return form;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchToken(MultiValueMap<String, String> form, String unauthorizedMessage) {
        String tokenUrl = "%s/realms/%s/protocol/openid-connect/token"
                .formatted(keycloakAuthProperties.url(), keycloakAuthProperties.realm());

        try {
            Map<String, Object> tokenBody = keycloakRestClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
            }
            return tokenBody;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthorizedMessage);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication service unavailable");
        }
    }

    private String normalizeProviderAlias(String provider) {
        String normalized = provider.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "google", "facebook", "microsoft" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported social provider");
        };
    }
}
