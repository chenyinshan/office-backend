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
 * 站内通知接口代理：带 X-User-Id 转发到 user-service。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationProxyController {

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

    @GetMapping("/unread-count")
    public ResponseEntity<String> unreadCount(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notifications/unread-count",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping
    public ResponseEntity<String> list(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notifications",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<String> markRead(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notifications/" + id + "/read",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }
}
