package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门表 sys_dept
 */
@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Integer sortOrder;
    private Long leaderEmployeeId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
