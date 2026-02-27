package com.example.oa.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待办任务 + 流程实例标题 + 业务详情（请假/报销）。
 */
@Data
public class TaskDetailVo {
    private Long id;
    private Long instanceId;
    private String nodeKey;
    /** 节点名称（如 部门主管审批、人事审批） */
    private String nodeName;
    private Long assigneeUserId;
    private String status;
    private String resultComment;
    private LocalDateTime actedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String title;
    private String businessType;

    /** 请假：类型（annual/sick等） */
    private String leaveType;
    /** 请假：天数 */
    private BigDecimal leaveDays;
    /** 请假：原因 */
    private String leaveReason;
    /** 请假：开始/结束时间 */
    private LocalDateTime leaveStartTime;
    private LocalDateTime leaveEndTime;

    /** 报销：金额 */
    private BigDecimal expenseAmount;
    /** 报销：类型 */
    private String expenseType;
    /** 报销：说明 */
    private String expenseDescription;
}
