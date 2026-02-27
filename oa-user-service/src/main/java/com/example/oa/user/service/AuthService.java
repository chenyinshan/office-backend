package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.LoginResponse;
import com.example.oa.user.dto.UserInfoVo;
import com.example.oa.user.entity.SysDept;
import com.example.oa.user.entity.SysEmployee;
import com.example.oa.user.entity.SysUserAccount;
import com.example.oa.user.mapper.SysDeptMapper;
import com.example.oa.user.mapper.SysEmployeeMapper;
import com.example.oa.user.mapper.SysPostMapper;
import com.example.oa.user.mapper.SysUserAccountMapper;
import com.example.oa.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录与当前用户：校验密码、发放 token、查询用户信息。
 * token 当前为内存 Map，后续可改为 Redis 或 JWT。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserAccountMapper userAccountMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysEmployeeMapper employeeMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final PasswordEncoder passwordEncoder;

    /** 简单内存 token 存储：token -> userId，后续可迁到 Redis */
    private static final Map<String, Long> TOKEN_STORE = new ConcurrentHashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(String username, String password) {
        SysUserAccount account = userAccountMapper.selectOne(
                Wrappers.<SysUserAccount>lambdaQuery().eq(SysUserAccount::getUsername, username).last("LIMIT 1"));
        if (account == null) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (account.getStatus() != null && account.getStatus() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        userAccountMapper.update(null, Wrappers.<SysUserAccount>lambdaUpdate()
                .eq(SysUserAccount::getId, account.getId())
                .set(SysUserAccount::getLastLoginAt, LocalDateTime.now()));

        String token = UUID.randomUUID().toString();
        TOKEN_STORE.put(token, account.getId());

        UserInfoVo user = buildUserInfo(account.getId());
        return LoginResponse.builder().token(token).user(user).build();
    }

    /**
     * 根据 token 获取当前用户信息；token 无效或过期返回 null（由 Controller 抛 401）
     */
    public UserInfoVo getCurrentUser(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Long userId = TOKEN_STORE.get(token.trim());
        if (userId == null) {
            return null;
        }
        return buildUserInfo(userId);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            TOKEN_STORE.remove(token.trim());
        }
    }

    private UserInfoVo buildUserInfo(Long userId) {
        SysUserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        SysEmployee employee = employeeMapper.selectById(account.getEmployeeId());
        if (employee == null) {
            List<String> roles = userRoleMapper.selectRoleCodesByUserId(account.getId());
            return UserInfoVo.builder()
                    .userId(account.getId())
                    .username(account.getUsername())
                    .employeeId(account.getEmployeeId())
                    .name("未知")
                    .roles(roles != null ? roles : List.of())
                    .build();
        }
        String deptName = null;
        String postName = null;
        if (employee.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(employee.getDeptId());
            if (dept != null) {
                deptName = dept.getDeptName();
            }
        }
        if (employee.getPostId() != null) {
            var post = postMapper.selectById(employee.getPostId());
            if (post != null) {
                postName = post.getPostName();
            }
        }
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(account.getId());
        return UserInfoVo.builder()
                .userId(account.getId())
                .username(account.getUsername())
                .employeeId(employee.getId())
                .employeeNo(employee.getEmployeeNo())
                .name(employee.getName())
                .deptId(employee.getDeptId())
                .deptName(deptName)
                .postId(employee.getPostId())
                .postName(postName)
                .roles(roles != null ? roles : List.of())
                .build();
    }
}
