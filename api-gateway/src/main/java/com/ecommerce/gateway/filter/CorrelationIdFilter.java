package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propagates a correlation ID through every downstream request.
 *
 * If the inbound request already carries an X-Correlation-Id header (e.g. from
 * another upstream system), that value is preserved. Otherwise a new UUID is
 * generated. The ID is added to both the forwarded request and the response.
 *
 * Downstream services should read X-Correlation-Id and propagate it further
 * in any outbound calls (Feign, Kafka headers, etc.).
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
                                       .getHeaders()
                                       .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        String cid = correlationId;

        // Mutate the request to carry the correlation ID downstream
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, cid)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, cid);

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Execute early so all downstream filters see the correlation ID
        return -100;
    }
}