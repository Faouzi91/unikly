package com.unikly.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive global filter that propagates the X-User-Id header value into
 * SLF4J MDC so all log lines emitted by the gateway include the userId field.
 * Runs after {@link JwtHeaderRelayFilter} (order 1) so the header is already set.
 */
@Component
public class MdcLoggingFilter implements GlobalFilter, Ordered {

    private static final String MDC_USER_ID_KEY = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        return chain.filter(exchange)
                .doFirst(() -> {
                    if (userId != null) {
                        MDC.put(MDC_USER_ID_KEY, userId);
                    }
                })
                .doFinally(signal -> MDC.remove(MDC_USER_ID_KEY));
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
