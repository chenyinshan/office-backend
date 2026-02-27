package com.example.oa.user.controller;

import com.example.oa.common.BizException;
import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.NotificationVo;
import com.example.oa.user.entity.OaNotification;
import com.example.oa.user.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内通知：创建（供 portal 在审批后调用）、列表、未读数、标记已读。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final NotificationService notificationService;

    private Long requireUserId(HttpServletRequest request) {
        String v = request.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) throw new BizException(ResultCode.UNAUTHORIZED);
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    /** 创建一条通知给指定用户（portal 在审批通过/驳回后调用，带当前用户 token） */
    @PostMapping
    public Result<OaNotification> create(@RequestBody CreateNotificationRequest req, HttpServletRequest request) {
        requireUserId(request);
        OaNotification n = notificationService.create(
                req.getTargetUserId(),
                req.getType(),
                req.getTitle(),
                req.getContent(),
                req.getBusinessType(),
                req.getBusinessId());
        return Result.ok(n);
    }

    /** 未读数量（须在 GET /{id} 前） */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(notificationService.unreadCount(userId));
    }

    /** 我的通知列表 */
    @GetMapping
    public Result<List<NotificationVo>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(notificationService.listByUser(userId));
    }

    /** 标记已读 */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    @lombok.Data
    public static class CreateNotificationRequest {
        private Long targetUserId;
        private String type;
        private String title;
        private String content;
        private String businessType;
        private String businessId;
    }
}
