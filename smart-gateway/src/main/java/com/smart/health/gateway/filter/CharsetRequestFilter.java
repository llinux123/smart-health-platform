package com.smart.health.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class CharsetRequestFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(this::addCharsetHeaders)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private void addCharsetHeaders(HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        if (contentType != null && contentType.getCharset() == null) {
            if ("multipart".equals(contentType.getType())) {
                return;
            }
            headers.setContentType(new MediaType(
                    contentType.getType(),
                    contentType.getSubtype(),
                    StandardCharsets.UTF_8));
        }

        if (headers.get("Accept-Charset") == null || headers.get("Accept-Charset").isEmpty()) {
            headers.set("Accept-Charset", StandardCharsets.UTF_8.name());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
