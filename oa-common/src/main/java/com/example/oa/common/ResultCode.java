package com.example.oa.common;

import lombok.Getter;

/**
 * 统一错误码枚举。
 * 与前端约定：code=0 表示成功，非 0 为业务或系统错误。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),

    // 通用 1xxxx
    BAD_REQUEST(10001, "请求参数错误"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限"),
    NOT_FOUND(10004, "资源不存在"),
    INTERNAL_ERROR(10999, "系统繁忙，请稍后重试"),

    // 用户/组织 2xxxx
    USER_NOT_FOUND(20001, "用户不存在"),
    USER_DISABLED(20002, "账号已禁用"),
    USERNAME_OR_PASSWORD_ERROR(20003, "用户名或密码错误"),
    DEPT_NOT_FOUND(20004, "部门不存在"),
    EMPLOYEE_NOT_FOUND(20005, "员工不存在"),

    // 审批流 3xxxx
    WORKFLOW_DEFINITION_NOT_FOUND(30001, "流程定义不存在"),
    WORKFLOW_INSTANCE_NOT_FOUND(30002, "流程实例不存在"),
    WORKFLOW_TASK_NOT_FOUND(30003, "待办任务不存在"),
    WORKFLOW_ALREADY_FINISHED(30004, "流程已结束"),
    WORKFLOW_NOT_PENDING(30005, "当前任务已处理，无法重复操作");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
