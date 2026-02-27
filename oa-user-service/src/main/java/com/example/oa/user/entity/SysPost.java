package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 岗位表 sys_post */
@Data
@TableName("sys_post")
public class SysPost {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String postCode;
    private String postName;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
