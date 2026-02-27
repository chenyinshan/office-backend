package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_node")
public class WfNode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long definitionId;
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String approverType;
    /** JSON，如 {"assignee_user_id": 2} */
    private String approverConfig;
    /** JSON 数组，如 ["dept_leader"] */
    private String nextNodeKeys;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
