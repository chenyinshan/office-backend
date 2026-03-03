package com.example.oa.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常 BizException 的单元测试。
 */
class BizExceptionTest {

    @Test
    void constructor_withResultCode() {
        BizException e = new BizException(ResultCode.UNAUTHORIZED);
        assertEquals(ResultCode.UNAUTHORIZED, e.getResultCode());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), e.getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getMessage(), e.getMessage());
        assertNull(e.getOverrideMessage());
    }

    @Test
    void constructor_withResultCodeAndOverrideMessage() {
        BizException e = new BizException(ResultCode.FORBIDDEN, "无权限操作");
        assertEquals(ResultCode.FORBIDDEN, e.getResultCode());
        assertEquals("无权限操作", e.getMessage());
        assertEquals("无权限操作", e.getOverrideMessage());
    }

    @Test
    void constructor_withResultCodeAndNullOverrideMessage() {
        BizException e = new BizException(ResultCode.NOT_FOUND, (String) null);
        assertEquals(ResultCode.NOT_FOUND.getMessage(), e.getMessage());
    }

    @Test
    void constructor_withResultCodeAndCause() {
        Throwable cause = new RuntimeException("cause");
        BizException e = new BizException(ResultCode.INTERNAL_ERROR, cause);
        assertEquals(ResultCode.INTERNAL_ERROR, e.getResultCode());
        assertSame(cause, e.getCause());
    }
}
