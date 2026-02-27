package com.example.oa.user.controller;

import com.example.oa.common.BizException;
import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.NoticeVo;
import com.example.oa.user.entity.OaNotice;
import com.example.oa.user.service.NoticeService;
import com.example.oa.user.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告：列表、详情、发布、标记已读。当前用户通过 X-User-Id 传递（由 portal 转发时填入）。
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final NoticeService noticeService;
    private final RoleService roleService;

    private Long requireUserId(HttpServletRequest request) {
        String v = request.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) throw new BizException(ResultCode.UNAUTHORIZED);
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    /** 已发布公告列表（置顶优先、发布时间倒序） */
    @GetMapping
    public Result<List<NoticeVo>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(noticeService.listPublished(userId));
    }

    /** 我的草稿列表（需 ADMIN 角色，须在 GET /{id} 之前避免被路径匹配） */
    @GetMapping("/my-drafts")
    public Result<List<NoticeVo>> myDrafts(HttpServletRequest request) {
        Long userId = requireUserId(request);
        roleService.requirePublishNotice(userId);
        return Result.ok(noticeService.listMyDrafts(userId));
    }

    /** 公告详情；?read=1 时同时标记已读 */
    @GetMapping("/{id}")
    public Result<NoticeVo> getById(@PathVariable("id") Long id, @RequestParam(name = "read", defaultValue = "0") int read, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(noticeService.getById(id, userId, read == 1));
    }

    /** 发布公告（需 ADMIN 角色） */
    @PostMapping
    public Result<OaNotice> publish(@RequestBody PublishRequest req, HttpServletRequest request) {
        Long userId = requireUserId(request);
        roleService.requirePublishNotice(userId);
        OaNotice n = noticeService.publish(userId, req.getTitle(), req.getContent(), req.getIsTop() != null && req.getIsTop());
        return Result.ok(n);
    }

    /** 保存草稿（需 ADMIN 角色） */
    @PostMapping("/draft")
    public Result<OaNotice> saveDraft(@RequestBody PublishRequest req, HttpServletRequest request) {
        Long userId = requireUserId(request);
        roleService.requirePublishNotice(userId);
        OaNotice n = noticeService.saveDraft(userId, req.getTitle(), req.getContent(), req.getIsTop() != null && req.getIsTop());
        return Result.ok(n);
    }

    /** 更新公告（草稿或已发布的作者可改） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody UpdateRequest req, HttpServletRequest request) {
        Long userId = requireUserId(request);
        noticeService.update(id, userId, req.getTitle(), req.getContent(), req.getIsTop());
        return Result.ok();
    }

    /** 发布草稿（仅本人草稿） */
    @PostMapping("/{id}/publish")
    public Result<Void> publishDraft(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        noticeService.publishDraft(id, userId);
        return Result.ok();
    }

    /** 下架公告（已发布改为草稿，本人或 ADMIN） */
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        boolean isAdmin = roleService.hasAnyRole(userId, "ADMIN");
        noticeService.unpublish(id, userId, isAdmin);
        return Result.ok();
    }

    /** 删除：草稿仅作者可删，已发布仅 ADMIN 可删 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        boolean isAdmin = roleService.hasAnyRole(userId, "ADMIN");
        noticeService.delete(id, userId, isAdmin);
        return Result.ok();
    }

    /** 标记已读 */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        noticeService.markRead(id, userId);
        return Result.ok();
    }

    @lombok.Data
    public static class PublishRequest {
        private String title;
        private String content;
        private Boolean isTop;
    }

    @lombok.Data
    public static class UpdateRequest {
        private String title;
        private String content;
        private Boolean isTop;
    }
}
