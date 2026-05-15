package com.team14.amazon.gateway.filter;

import com.team14.amazon.gateway.security.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Bypass JWT validation for auth and health endpoints
        if (path.startsWith("/api/auth/") || path.endsWith("/health")) {
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-Correlation-ID", finalCorrelationId))
                    .build();
            return chain.filter(mutated);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.get("uid") != null ? claims.get("uid").toString() : "";
            String role = claims.get("role", String.class);
            if (role == null) role = "";

            String finalUserId = userId;
            String finalRole = role;

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Id", finalUserId)
                            .header("X-User-Role", finalRole)
                            .header("X-Correlation-ID", finalCorrelationId))
                    .build();

            return chain.filter(mutated);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = JwtConfigurationManager.getInstance().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
