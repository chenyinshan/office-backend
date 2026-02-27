package com.example.oa.portal.controller;

import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.portal.client.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 组织管理接口代理：/api/org/** 带 X-User-Id 转发到 user-service。
 */
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgProxyController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final RestTemplate restTemplate;
    private final UserServiceClient userServiceClient;

    @Value("${app.user-service-url:http://localhost:8082}")
    private String userServiceUrl;

    private String resolveToken(HttpServletRequest request) {
        String v = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (v == null || v.isBlank()) return null;
        v = v.trim();
        return v.startsWith("Bearer ") ? v.substring(7).trim() : v;
    }

    private HttpHeaders headersWithUserId(String token) {
        Long userId = userServiceClient.getUserIdFromToken(token);
        if (userId == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, String.valueOf(userId));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 从 request 中取出 /api/org 之后的路径与 query，避免 getRequestURI() 与 context-path 差异导致路径错误。
     */
    private String pathAndQuery(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/org";
        if (uri.startsWith(prefix)) {
            uri = uri.substring(prefix.length());
        } else if (uri.startsWith("api/org")) {
            uri = "/" + uri.substring("api/org".length());
        } else {
            uri = "/";
        }
        if (uri.isEmpty()) uri = "/";
        String q = request.getQueryString();
        return q != null ? uri + "?" + q : uri;
    }

    @GetMapping({ "", "/", "/{*path}" })
    public ResponseEntity<String> get(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        String url = userServiceUrl + "/api/org" + pathAndQuery(request);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    @PostMapping({ "", "/", "/{*path}" })
    public ResponseEntity<String> post(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserId(token));
        String url = userServiceUrl + "/api/org" + pathAndQuery(request);
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    @PutMapping({ "", "/", "/{*path}" })
    public ResponseEntity<String> put(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserId(token));
        String url = userServiceUrl + "/api/org" + pathAndQuery(request);
        return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    @DeleteMapping({ "", "/", "/{*path}" })
    public ResponseEntity<String> delete(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        String url = userServiceUrl + "/api/org" + pathAndQuery(request);
        return restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }
}
