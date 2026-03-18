package com.unikly.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtHeaderRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/webhooks/")) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal())
                .filter(Jwt.class::isInstance)
                .cast(Jwt.class)
                .flatMap(jwt -> {
                    String userId = jwt.getSubject();
                    String roles = extractRoles(jwt);

                    var mutated = exchange.mutate()
                            .request(r -> r
                                    .header("X-User-Id", userId)
                                    .header("X-User-Roles", roles))
                            .build();

                    log.debug("Relaying JWT headers: userId={}, roles={}", userId, roles);
                    return chain.filter(mutated);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @SuppressWarnings("unchecked")
    private String extractRoles(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return "";
        }
        Object roles = realmAccess.get("roles");
        if (roles instanceof List<?> roleList) {
            return String.join(",", (List<String>) roleList);
        }
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
