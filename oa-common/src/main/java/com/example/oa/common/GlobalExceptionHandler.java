package com.example.oa.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：将 BizException、参数校验异常等转为统一 Result 返回。
 * 各服务模块需在启动类所在包或父包下扫描到本类（与 oa-common 同 group 即可）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        Result<Void> result = e.getOverrideMessage() != null
                ? Result.fail(e.getResultCode(), e.getOverrideMessage())
                : Result.fail(e.getResultCode());
        HttpStatus status = e.getCode() == ResultCode.UNAUTHORIZED.getCode() ? HttpStatus.UNAUTHORIZED
                : e.getCode() == ResultCode.FORBIDDEN.getCode() ? HttpStatus.FORBIDDEN
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

    /** 参数校验失败（@Valid 触发的 MethodArgumentNotValidException） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.BAD_REQUEST, message));
    }

    /** multipart 解析失败（如缺少 boundary、格式错误），返回 200 便于前端展示原因 */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> handleMultipart(MultipartException e) {
        log.warn("multipart parse failed: {}", e.getMessage());
        String msg = e.getMessage() != null ? e.getMessage() : "请求格式错误，请重试";
        return ResponseEntity.ok().body(Result.fail(ResultCode.BAD_REQUEST, "附件解析失败: " + msg));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Result<Void>> handleOther(Throwable e) {
        log.error("unhandled exception", e);
        String msg = e.getMessage() != null ? e.getMessage() : ResultCode.INTERNAL_ERROR.getMessage();
        return ResponseEntity.ok()
                .body(Result.fail(ResultCode.INTERNAL_ERROR, "系统异常: " + msg));
    }
}
