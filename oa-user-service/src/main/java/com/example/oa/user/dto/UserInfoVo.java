package com.example.oa.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户信息（登录返回 /api/me 返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {

    private Long userId;
    private String username;
    private Long employeeId;
    private String employeeNo;
    private String name;
    private Long deptId;
    private String deptName;
    private Long postId;
    private String postName;
    /** 角色编码列表，如 ["ADMIN","STAFF"]，用于前端/权限判断 */
    private java.util.List<String> roles;
}
