package com.smart.health.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class CharsetResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            MediaType contentType = headers.getContentType();
            if (contentType != null && contentType.getCharset() == null) {
                headers.setContentType(
                        new MediaType(contentType.getType(),
                                contentType.getSubtype(),
                                StandardCharsets.UTF_8));
            }
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
