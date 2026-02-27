package com.example.oa.portal.filter;

import com.example.oa.portal.client.UserServiceClient;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统一鉴权：除登录接口外，/api/** 均需有效 token，否则返回 401。
 */
@Component
@RequiredArgsConstructor
public class AuthFilter implements Filter {

    private final UserServiceClient userServiceClient;

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String API_PREFIX = "/api";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse resp)) {
            chain.doFilter(request, response);
            return;
        }
        String uri = req.getRequestURI();
        if (uri == null || !uri.startsWith(API_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        if ("POST".equalsIgnoreCase(req.getMethod()) && LOGIN_PATH.equals(uri)) {
            chain.doFilter(request, response);
            return;
        }
        String token = resolveToken(req);
        if (token == null || userServiceClient.getUserIdFromToken(token) == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getOutputStream().write(("{\"code\":10002,\"message\":\"未登录或登录已过期\",\"data\":null,\"success\":false}")
                    .getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String resolveToken(HttpServletRequest request) {
        String v = request.getHeader("Authorization");
        if (v == null || v.isBlank()) return null;
        v = v.trim();
        return v.startsWith("Bearer ") ? v.substring(7).trim() : v;
    }
}
