package com.example.oa.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流程实例 + 业务详情（请假/报销）。
 */
@Data
public class InstanceDetailVo {
    private Long id;
    private Long definitionId;
    private String processKey;
    private String businessType;
    private Long businessId;
    private String title;
    private Long applicantUserId;
    private Long applicantEmployeeId;
    private String status;
    private String currentNodeKey;
    /** 当前节点名称（如 部门主管审批、人事审批），仅 running 时有意义 */
    private String currentNodeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;

    /** 请假：类型、天数、原因、时间 */
    private String leaveType;
    private BigDecimal leaveDays;
    private String leaveReason;
    private LocalDateTime leaveStartTime;
    private LocalDateTime leaveEndTime;

    /** 报销：金额、类型、说明 */
    private BigDecimal expenseAmount;
    private String expenseType;
    private String expenseDescription;
}
