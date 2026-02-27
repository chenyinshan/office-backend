package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_task_log")
public class WfTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long instanceId;
    private String nodeKey;
    private Long operatorUserId;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
