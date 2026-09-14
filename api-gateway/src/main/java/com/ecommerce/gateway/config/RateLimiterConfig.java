package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Rate limiter configuration using Redis-backed token bucket algorithm.
 * Uses the client's IP address as the rate limit key.
 *
 * Each distinct IP gets its own token bucket with:
 *   - replenishRate: tokens added per second
 *   - burstCapacity: maximum tokens the bucket can hold
 *   - requestedTokens: tokens consumed per request
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> {
            String ip = "anonymous";
            if (exchange.getRequest().getRemoteAddress() != null) {
                ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                           .getAddress()
                           .getHostAddress();
            }
            return Mono.just(ip);
        };
    }
}