package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号表 sys_user_account
 */
@Data
@TableName("sys_user_account")
public class SysUserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private String username;
    private String passwordHash;
    private String salt;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
