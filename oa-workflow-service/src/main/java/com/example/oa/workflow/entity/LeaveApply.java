package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leave_apply")
public class LeaveApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long applicantUserId;
    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal days;
    private String reason;
    private String attachmentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
