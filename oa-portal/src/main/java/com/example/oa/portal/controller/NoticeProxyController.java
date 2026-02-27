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
 * 公告接口代理：从 token 解析 userId，带 X-User-Id 转发到 user-service。
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeProxyController {

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

    @GetMapping
    public ResponseEntity<String> list(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/my-drafts")
    public ResponseEntity<String> myDrafts(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/my-drafts",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getById(@PathVariable("id") Long id, @RequestParam(name = "read", required = false) Integer read, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        String url = userServiceUrl + "/api/notices/" + id + (read != null && read == 1 ? "?read=1" : "");
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping
    public ResponseEntity<String> publish(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/draft")
    public ResponseEntity<String> saveDraft(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/draft",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable("id") Long id, @RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/" + id,
                HttpMethod.PUT,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<String> publishDraft(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/" + id + "/publish",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<String> unpublish(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/" + id + "/unpublish",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserId(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                userServiceUrl + "/api/notices/" + id,
                HttpMethod.DELETE,
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
                userServiceUrl + "/api/notices/" + id + "/read",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }
}
