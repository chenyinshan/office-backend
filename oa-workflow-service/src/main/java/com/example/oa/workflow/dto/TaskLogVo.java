package com.example.oa.workflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批操作记录（用于实例详情页展示）。
 */
@Data
public class TaskLogVo {
    private Long id;
    private Long taskId;
    private Long instanceId;
    private String nodeKey;
    private Long operatorUserId;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
