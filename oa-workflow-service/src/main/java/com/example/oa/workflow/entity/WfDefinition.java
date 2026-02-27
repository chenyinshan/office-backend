package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_definition")
public class WfDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String processKey;
    private String processName;
    private Integer version;
    private String formSchema;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
