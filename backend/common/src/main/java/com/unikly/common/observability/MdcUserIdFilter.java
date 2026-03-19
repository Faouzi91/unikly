package com.unikly.common.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;

/**
 * Servlet filter that extracts the current user ID from the JWT SecurityContext
 * (or the X-User-Id gateway header) and puts it into SLF4J MDC under "userId".
 * Cleared after every request so MDC does not leak across threads.
 */
public class MdcUserIdFilter implements Filter {

    private static final String MDC_KEY = "userId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String userId = extractUserId((HttpServletRequest) request);
            if (userId != null) {
                MDC.put(MDC_KEY, userId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String extractUserId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        String header = request.getHeader("X-User-Id");
        return (header != null && !header.isBlank()) ? header : null;
    }
}
