package com.unikly.common.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.IOException;

@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = ((HttpServletResponse) response).getStatus();
            String userId = MDC.get("userId");
            log.info("{} {} user={} status={} duration={}ms",
                    req.getMethod(),
                    req.getRequestURI(),
                    userId != null ? userId : "anonymous",
                    status,
                    duration);
        }
    }
}
