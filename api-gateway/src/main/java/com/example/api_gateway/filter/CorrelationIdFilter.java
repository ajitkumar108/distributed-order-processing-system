package com.example.api_gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * GLOBAL FILTER: runs for EVERY request that passes through the gateway.
 *
 * Purpose: make sure each request carries an "X-Correlation-Id" header. This ID follows the request
 * into Order Service so you can grep one ID in all logs and see the whole journey.
 * (If the client already sent one, we keep it; otherwise we generate a new UUID.)
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    /** Name of the HTTP header that carries the correlation id. */
    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1) Read the header the client sent (may be null).
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);

        // 2) If missing/blank, create a fresh id.
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 3) Requests are immutable, so build a modified copy that has the header set.
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HEADER, correlationId)
                .build();

        // 4) Also return the id to the caller in the response headers (handy when debugging from Postman).
        exchange.getResponse().getHeaders().set(HEADER, correlationId);

        log.info("[{}] {} {}", correlationId, exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());

        // 5) Continue down the filter chain using the modified request.
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run first, before any other filter, so everything after sees the header.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
