package com.example.oa.common;

import lombok.Getter;

import java.io.Serializable;

/**
 * 统一 API 返回体。
 * 与前端约定：code=0 成功，data 为业务数据；非 0 为错误，message 为提示文案。
 *
 * @param <T> 业务数据类型
 */
@Getter
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String overrideMessage) {
        return new Result<>(resultCode.getCode(), overrideMessage != null ? overrideMessage : resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 是否成功（code == 0） */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode() == code;
    }
}
