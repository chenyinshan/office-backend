package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_task")
public class WfTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private String nodeKey;
    private Long assigneeUserId;
    private String status;
    private String resultComment;
    private LocalDateTime actedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
