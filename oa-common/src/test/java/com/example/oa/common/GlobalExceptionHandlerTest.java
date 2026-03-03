package com.example.oa.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器 GlobalExceptionHandler 的单元测试。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBizException_usesEnumMessage() {
        BizException e = new BizException(ResultCode.UNAUTHORIZED);
        ResponseEntity<Result<Void>> res = handler.handleBizException(e);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().isSuccess());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), res.getBody().getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getMessage(), res.getBody().getMessage());
    }

    @Test
    void handleBizException_usesOverrideMessage() {
        BizException e = new BizException(ResultCode.BAD_REQUEST, "参数不能为空");
        ResponseEntity<Result<Void>> res = handler.handleBizException(e);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), res.getBody().getCode());
        assertEquals("参数不能为空", res.getBody().getMessage());
    }

    @Test
    void handleBizException_forbidden_returns403() {
        BizException e = new BizException(ResultCode.FORBIDDEN);
        ResponseEntity<Result<Void>> res = handler.handleBizException(e);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void handleOther_returnsInternalError() {
        Throwable t = new RuntimeException("test error");
        ResponseEntity<Result<Void>> res = handler.handleOther(t);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), res.getBody().getCode());
        assertTrue(res.getBody().getMessage().contains("test error"));
    }

    @Test
    void handleOther_nullMessage_usesDefault() {
        ResponseEntity<Result<Void>> res = handler.handleOther(new RuntimeException());
        assertEquals(ResultCode.INTERNAL_ERROR.getCode(), res.getBody().getCode());
        assertTrue(res.getBody().getMessage().startsWith("系统异常"));
    }
}
