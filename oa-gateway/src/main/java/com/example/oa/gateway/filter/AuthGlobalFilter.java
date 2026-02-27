package com.example.oa.gateway.filter;

import com.example.oa.common.ResultCode;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关统一鉴权：除登录接口外，/api/** 必须带有效 Authorization: Bearer &lt;token&gt;，否则 401。
 * 不校验 token 内容（由 portal / user-service 校验），只校验存在且非空。
 */
@Component
public class AuthGlobalFilter implements org.springframework.cloud.gateway.filter.GlobalFilter, Ordered {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String API_PREFIX = "/api/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }
        if (LOGIN_PATH.equals(path) && "POST".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            return writeUnauthorized(exchange);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"success\":false}",
                ResultCode.UNAUTHORIZED.getCode(),
                ResultCode.UNAUTHORIZED.getMessage().replace("\"", "\\\""));
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
