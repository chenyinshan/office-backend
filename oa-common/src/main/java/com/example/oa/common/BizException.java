package com.example.oa.common;

import lombok.Getter;

/**
 * 业务异常，携带错误码。
 * Controller 层或全局异常处理器捕获后转为 Result.fail 返回前端。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ResultCode resultCode;
    private final String overrideMessage;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.overrideMessage = null;
    }

    public BizException(ResultCode resultCode, String overrideMessage) {
        super(overrideMessage != null ? overrideMessage : resultCode.getMessage());
        this.resultCode = resultCode;
        this.overrideMessage = overrideMessage;
    }

    public BizException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
        this.overrideMessage = null;
    }

    /** 供全局异常处理取 code 与 message */
    public int getCode() {
        return resultCode.getCode();
    }

    @Override
    public String getMessage() {
        return overrideMessage != null ? overrideMessage : resultCode.getMessage();
    }
}
