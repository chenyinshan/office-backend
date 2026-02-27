package com.example.oa.portal.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 调 user-service /api/auth/me，根据 token 解析出 userId、employeeId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.user-service-url:http://localhost:8082}")
    private String userServiceUrl;

    /**
     * 用 token 调 /api/auth/me，成功时返回 userId，失败或未登录返回 null。
     */
    public Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    userServiceUrl + "/api/auth/me",
                    HttpMethod.GET,
                    entity,
                    String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
                Object code = body.get("code");
                if (code != null && Integer.valueOf(0).equals(Integer.valueOf(code.toString()))) {
                    Object data = body.get("data");
                    if (data instanceof Map) {
                        Object userId = ((Map<?, ?>) data).get("userId");
                        if (userId instanceof Number) return ((Number) userId).longValue();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("user-service /api/auth/me 调用失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 同 getUserIdFromToken，并返回 employeeId（用于 X-Employee-Id）；无则返回 null。
     */
    public long[] getUserIdAndEmployeeIdFromToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    userServiceUrl + "/api/auth/me",
                    HttpMethod.GET,
                    entity,
                    String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
                Object code = body.get("code");
                if (code != null && Integer.valueOf(0).equals(Integer.valueOf(code.toString()))) {
                    Object data = body.get("data");
                    if (data instanceof Map) {
                        Map<?, ?> d = (Map<?, ?>) data;
                        Long userId = toLong(d.get("userId"));
                        Long employeeId = toLong(d.get("employeeId"));
                        if (userId != null) {
                            return new long[]{userId, employeeId != null ? employeeId : userId};
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("user-service /api/auth/me 调用失败: {}", e.getMessage());
        }
        return null;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
