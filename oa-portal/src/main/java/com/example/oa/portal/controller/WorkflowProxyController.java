package com.example.oa.portal.controller;

import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.portal.client.UserServiceClient;
import com.example.oa.portal.filter.AttachmentUploadCachingFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 审批流接口代理：从 Authorization 解析 token，调 user-service 取 userId/employeeId，再带 X-User-Id、X-Employee-Id 转发到 workflow-service。
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowProxyController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_EMPLOYEE_ID = "X-Employee-Id";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final UserServiceClient userServiceClient;

    @Value("${app.workflow-service-url:http://localhost:8083}")
    private String workflowServiceUrl;

    @Value("${app.user-service-url:http://localhost:8082}")
    private String userServiceUrl;

    private String resolveToken(HttpServletRequest request) {
        String v = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (v == null || v.isBlank()) return null;
        v = v.trim();
        return v.startsWith("Bearer ") ? v.substring(7).trim() : v;
    }

    private HttpHeaders headersWithUser(String token) {
        long[] ids = userServiceClient.getUserIdAndEmployeeIdFromToken(token);
        if (ids == null || ids.length < 1) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_USER_ID, String.valueOf(ids[0]));
        if (ids.length > 1) headers.set(HEADER_EMPLOYEE_ID, String.valueOf(ids[1]));
        return headers;
    }

    private HttpHeaders headersWithUserJson(String token) {
        HttpHeaders h = headersWithUser(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @PostMapping("/leave/start")
    public ResponseEntity<String> startLeave(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/leave/start",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/expense/start")
    public ResponseEntity<String> startExpense(@RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/expense/start",
                HttpMethod.POST,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/tasks/pending")
    public ResponseEntity<String> pendingTasks(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/tasks/pending",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/tasks/my")
    public ResponseEntity<String> myTasks(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserJson(token));
        String url = workflowServiceUrl + "/api/workflow/tasks/my";
        String q = request.getQueryString();
        if (q != null && !q.isBlank()) url = url + "?" + q;
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/instances/my")
    public ResponseEntity<String> myInstances(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/instances/my",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<String> getInstance(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/instances/" + id,
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/instances/{id}/logs")
    public ResponseEntity<String> getInstanceLogs(@PathVariable("id") Long id, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/instances/" + id + "/logs",
                HttpMethod.GET,
                entity,
                String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/stats/leave")
    public ResponseEntity<String> leaveStats(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUser(token));
        String url = workflowServiceUrl + "/api/workflow/stats/leave";
        String q = request.getQueryString();
        if (q != null && !q.isBlank()) url = url + "?" + q;
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @GetMapping("/stats/expense")
    public ResponseEntity<String> expenseStats(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<Void> entity = new HttpEntity<>(headersWithUser(token));
        String url = workflowServiceUrl + "/api/workflow/stats/expense";
        String q = request.getQueryString();
        if (q != null && !q.isBlank()) url = url + "?" + q;
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<String> approve(@PathVariable("taskId") Long taskId, @RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/tasks/" + taskId + "/approve",
                HttpMethod.POST,
                entity,
                String.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            notifyApplicant(token, resp.getBody(), true);
        }
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<String> reject(@PathVariable("taskId") Long taskId, @RequestBody(required = false) String body, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
        HttpEntity<String> entity = new HttpEntity<>(body != null ? body : "{}", headersWithUserJson(token));
        ResponseEntity<String> resp = restTemplate.exchange(
                workflowServiceUrl + "/api/workflow/tasks/" + taskId + "/reject",
                HttpMethod.POST,
                entity,
                String.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            notifyApplicant(token, resp.getBody(), false);
        }
        return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
    }

    /** 解析审批结果并给发起人发站内通知（失败仅打日志，不影响审批响应） */
    private void notifyApplicant(String token, String workflowResponseBody, boolean approved) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(workflowResponseBody);
            if (root == null || !root.path("data").isObject()) return;
            JsonNode data = root.get("data");
            Long applicantUserId = data.path("applicantUserId").asLong(0);
            Long instanceId = data.path("instanceId").asLong(0);
            String businessType = data.path("businessType").asText("workflow");
            String title = data.path("title").asText("");
            if (applicantUserId == 0 || instanceId == 0) return;
            String type = approved ? "workflow_approved" : "workflow_rejected";
            String noticeTitle = approved ? "您的「" + title + "」已通过" : "您的「" + title + "」已被驳回";
            Map<String, Object> req = Map.of(
                    "targetUserId", applicantUserId,
                    "type", type,
                    "title", noticeTitle,
                    "businessType", "workflow",
                    "businessId", String.valueOf(instanceId));
            HttpHeaders headers = headersWithUser(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(req), headers);
            restTemplate.exchange(userServiceUrl + "/api/notifications", HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.warn("审批后发站内通知失败: {}", e.getMessage());
        }
    }

    /** 附件上传：透传原始 multipart 请求体（Filter 已缓存 body + Content-Type 含 boundary）到 workflow-service */
    @PostMapping("/attachments/upload")
    public ResponseEntity<String> uploadAttachment(HttpServletRequest request) {
        try {
            String token = resolveToken(request);
            if (token == null) throw new BizException(ResultCode.UNAUTHORIZED);
            byte[] body = AttachmentUploadCachingFilter.getCachedBody(request);
            String contentType = AttachmentUploadCachingFilter.getCachedContentType(request);
            if (body == null || body.length == 0 || contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonFail(ResultCode.BAD_REQUEST.getCode(), "请求体为空或不是 multipart"));
            }
            HttpHeaders headers = headersWithUser(token);
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
            HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    workflowServiceUrl + "/api/workflow/attachments/upload",
                    HttpMethod.POST,
                    entity,
                    String.class);
            if (resp.getStatusCode().is5xxServerError()) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonFail(10999, "附件服务异常，请确认 MinIO 与审批流服务已启动"));
            }
            return ResponseEntity.status(resp.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.getBody());
        } catch (BizException e) {
            throw e;
        } catch (Throwable e) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonFail(ResultCode.INTERNAL_ERROR.getCode(), "附件上传失败: " + e.getMessage()));
        }
    }

    private static String jsonFail(int code, String message) {
        String esc = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"code\":" + code + ",\"message\":\"" + esc + "\",\"data\":null,\"success\":false}";
    }
}
