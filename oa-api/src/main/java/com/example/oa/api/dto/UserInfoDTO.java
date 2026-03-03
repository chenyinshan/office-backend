package com.example.oa.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 当前用户信息 DTO，用于 Dubbo 跨服务传递（与 user-service 的 UserInfoVo 字段对齐）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private Long employeeId;
    private String employeeNo;
    private String name;
    private Long deptId;
    private String deptName;
    private Long postId;
    private String postName;
    /** 角色编码列表，如 ["ADMIN","STAFF"] */
    private List<String> roles;
}
