package com.example.oa.workflow.controller;

import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.common.BizException;
import com.example.oa.workflow.entity.OaAttachment;
import com.example.oa.workflow.service.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 附件上传。需请求头 X-User-Id（由 portal 转发时填入）。
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class AttachmentController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final AttachmentService attachmentService;

    private Long requireUserId(HttpServletRequest request) {
        String v = request.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) throw new BizException(ResultCode.UNAUTHORIZED);
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    /**
     * 上传单个文件。businessType 取 query：leave / expense，默认 leave。
     * 捕获所有异常，统一返回 200 + Result 避免 500。
     */
    @PostMapping("/attachments/upload")
    public Result<OaAttachment> upload(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "businessType", defaultValue = "leave") String businessType,
            HttpServletRequest request) {
        try {
            Long userId = requireUserId(request);
            if (file == null || file.isEmpty()) {
                return Result.fail(ResultCode.BAD_REQUEST, "请选择文件");
            }
            if (!"leave".equals(businessType) && !"expense".equals(businessType)) {
                businessType = "leave";
            }
            OaAttachment att = attachmentService.upload(file, businessType, userId);
            return Result.ok(att);
        } catch (BizException e) {
            return e.getOverrideMessage() != null
                    ? Result.fail(e.getResultCode(), e.getOverrideMessage())
                    : Result.fail(e.getResultCode());
        } catch (Throwable e) {
            return Result.fail(ResultCode.INTERNAL_ERROR, "上传失败: " + e.getMessage());
        }
    }
}
