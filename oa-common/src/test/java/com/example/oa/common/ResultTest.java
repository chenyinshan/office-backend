package com.example.oa.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一返回体 Result 的单元测试。
 */
class ResultTest {

    @Test
    void ok_withoutData() {
        Result<Object> r = Result.ok();
        assertTrue(r.isSuccess());
        assertEquals(ResultCode.SUCCESS.getCode(), r.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void ok_withData() {
        Result<String> r = Result.ok("hello");
        assertTrue(r.isSuccess());
        assertEquals(0, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void fail_withResultCode() {
        Result<Object> r = Result.fail(ResultCode.UNAUTHORIZED);
        assertFalse(r.isSuccess());
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), r.getCode());
        assertEquals(ResultCode.UNAUTHORIZED.getMessage(), r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void fail_withResultCodeAndOverrideMessage() {
        Result<Object> r = Result.fail(ResultCode.BAD_REQUEST, "自定义错误信息");
        assertFalse(r.isSuccess());
        assertEquals(ResultCode.BAD_REQUEST.getCode(), r.getCode());
        assertEquals("自定义错误信息", r.getMessage());
    }

    @Test
    void fail_withResultCodeAndNullOverrideMessage_usesEnumMessage() {
        Result<Object> r = Result.fail(ResultCode.NOT_FOUND, null);
        assertEquals(ResultCode.NOT_FOUND.getMessage(), r.getMessage());
    }

    @Test
    void fail_withCodeAndMessage() {
        Result<Object> r = Result.fail(100, "任意错误");
        assertFalse(r.isSuccess());
        assertEquals(100, r.getCode());
        assertEquals("任意错误", r.getMessage());
    }
}
