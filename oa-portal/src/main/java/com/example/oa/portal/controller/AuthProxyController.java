package com.example.oa.portal.controller;

import com.example.oa.common.ResultCode;
import com.example.oa.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 认证接口代理：转发到 user-service，路径与 user-service 一致，前端统一调 portal 即可。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthProxyController {

    private final RestTemplate restTemplate;

    @Value("${app.user-service-url:http://localhost:8082}")
    private String userServiceUrl;

    private static String resolveToken(HttpServletRequest request) {
        String v = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (v == null || v.isBlank()) return null;
        v = v.trim();
        return v.startsWith("Bearer ") ? v.substring(7).trim() : v;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody String body, @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/auth/login",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String token = resolveToken(request);
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/auth/logout",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/me")
    public ResponseEntity<String> me(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/auth/me",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }
}
