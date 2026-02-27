package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_instance")
public class WfInstance {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
}
