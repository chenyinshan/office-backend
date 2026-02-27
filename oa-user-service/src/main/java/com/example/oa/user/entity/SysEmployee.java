package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工表 sys_employee
 */
@Data
@TableName("sys_employee")
public class SysEmployee {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private Long postId;
    private String employeeNo;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private LocalDate hireDate;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
