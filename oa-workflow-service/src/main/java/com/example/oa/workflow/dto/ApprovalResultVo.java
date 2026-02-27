package com.example.oa.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批/驳回后返回给 portal，用于给发起人发站内通知。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResultVo {

    private Long applicantUserId;
    private Long instanceId;
    private String businessType;
    private String title;
    /** true=通过 false=驳回 */
    private Boolean approved;
}
