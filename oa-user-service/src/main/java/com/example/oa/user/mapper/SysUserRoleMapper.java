package com.example.oa.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 查询用户角色编码（用于 RBAC）。
 * sys_user_role.user_id = sys_user_account.id
 */
@Mapper
public interface SysUserRoleMapper {

    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND (r.status IS NULL OR r.status = 1)")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
