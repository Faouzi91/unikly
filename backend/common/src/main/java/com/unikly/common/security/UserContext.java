package com.unikly.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class UserContext {

    private UserContext() {
    }

    /**
     * Extracts the current user's ID from the JWT SecurityContext or the X-User-Id header
     * (set by the API Gateway).
     */
    public static UUID getUserId() {
        // Try SecurityContext JWT first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }

        // Fallback to gateway-injected header
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String headerValue = request.getHeader("X-User-Id");
            if (headerValue != null && !headerValue.isBlank()) {
                return UUID.fromString(headerValue);
            }
        }

        throw new IllegalStateException("No authenticated user found in SecurityContext or X-User-Id header");
    }

    /**
     * Extracts the current user's roles from the JWT SecurityContext or the X-User-Roles header.
     */
    public static List<String> getRoles() {
        // Try SecurityContext JWT first
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                return roles;
            }
        }

        // Fallback to gateway-injected header
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String headerValue = request.getHeader("X-User-Roles");
            if (headerValue != null && !headerValue.isBlank()) {
                return Arrays.asList(headerValue.split(","));
            }
        }

        return Collections.emptyList();
    }

    /**
     * Checks whether the current user has a specific role.
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * Throws if the current user does not have the required role.
     */
    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new SecurityException("Missing required role: " + role);
        }
    }

    private static HttpServletRequest getCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
