package com.example.oa.workflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LeaveStartRequest {

    @NotNull(message = "请假类型不能为空")
    private String leaveType;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    @NotNull(message = "天数不能为空")
    @DecimalMin("0.5")
    private BigDecimal days;
    private String reason;
    /** 附件 id 列表，逗号分隔，如 "1,2,3" */
    private java.util.List<Long> attachmentIds;
}
