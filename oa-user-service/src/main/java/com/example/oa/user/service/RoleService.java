package com.example.oa.user.service;

import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色校验，用于 RBAC。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysUserRoleMapper userRoleMapper;

    /** 用户是否拥有任意一个指定角色 */
    public boolean hasAnyRole(Long userId, String... roleCodes) {
        if (userId == null || roleCodes == null || roleCodes.length == 0) return false;
        List<String> userRoles = userRoleMapper.selectRoleCodesByUserId(userId);
        if (userRoles == null) return false;
        for (String code : roleCodes) {
            if (userRoles.contains(code)) return true;
        }
        return false;
    }

    /** 发布公告需 ADMIN 角色，否则抛 FORBIDDEN */
    public void requirePublishNotice(Long userId) {
        if (!hasAnyRole(userId, "ADMIN")) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限发布公告");
        }
    }
}
