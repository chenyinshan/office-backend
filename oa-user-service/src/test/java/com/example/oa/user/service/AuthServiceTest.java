package com.example.oa.user.service;

import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.LoginResponse;
import com.example.oa.user.dto.UserInfoVo;
import com.example.oa.user.entity.SysDept;
import com.example.oa.user.entity.SysEmployee;
import com.example.oa.user.entity.SysPost;
import com.example.oa.user.entity.SysUserAccount;
import com.example.oa.user.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

/**
 * AuthService 单元测试：登录、登出、当前用户。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserAccountMapper userAccountMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysEmployeeMapper employeeMapper;
    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private SysPostMapper postMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthTokenStore tokenStore;
    @Mock
    private TwoLevelUserInfoCache userInfoCache;

    @InjectMocks
    private AuthService authService;

    private static final Long USER_ID = 1L;
    private static final Long EMPLOYEE_ID = 10L;

    @BeforeEach
    void setUp() {
        // 每个用例前无共享 token 状态，依赖当前用例的 login 产生的 token
    }

    @Test
    void login_userNotFound_throwsUsernameOrPasswordError() {
        when(userAccountMapper.selectOne(any())).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> authService.login("unknown", "any"));
        assertEquals(ResultCode.USERNAME_OR_PASSWORD_ERROR, e.getResultCode());
    }

    @Test
    void login_userDisabled_throwsUserDisabled() {
        SysUserAccount account = new SysUserAccount();
        account.setId(USER_ID);
        account.setUsername("admin");
        account.setPasswordHash("hash");
        account.setStatus(0);
        when(userAccountMapper.selectOne(any())).thenReturn(account);

        BizException e = assertThrows(BizException.class,
                () -> authService.login("admin", "right"));
        assertEquals(ResultCode.USER_DISABLED, e.getResultCode());
    }

    @Test
    void login_wrongPassword_throwsUsernameOrPasswordError() {
        SysUserAccount account = new SysUserAccount();
        account.setId(USER_ID);
        account.setUsername("admin");
        account.setPasswordHash("hash");
        account.setStatus(1);
        when(userAccountMapper.selectOne(any())).thenReturn(account);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BizException e = assertThrows(BizException.class,
                () -> authService.login("admin", "wrong"));
        assertEquals(ResultCode.USERNAME_OR_PASSWORD_ERROR, e.getResultCode());
    }

    @Test
    void login_success_returnsTokenAndUser() {
        SysUserAccount account = new SysUserAccount();
        account.setId(USER_ID);
        account.setUsername("admin");
        account.setPasswordHash("hash");
        account.setStatus(1);
        account.setEmployeeId(EMPLOYEE_ID);
        when(userAccountMapper.selectOne(any())).thenReturn(account);
        when(passwordEncoder.matches(eq("pass"), eq("hash"))).thenReturn(true);
        when(userAccountMapper.selectById(USER_ID)).thenReturn(account);

        SysEmployee employee = new SysEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setName("张三");
        employee.setDeptId(1L);
        employee.setPostId(1L);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        SysDept dept = new SysDept();
        dept.setId(1L);
        dept.setDeptName("研发部");
        when(deptMapper.selectById(1L)).thenReturn(dept);

        SysPost post = new SysPost();
        post.setId(1L);
        post.setPostName("后端开发");
        when(postMapper.selectById(1L)).thenReturn(post);

        when(userRoleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of("ADMIN"));

        LoginResponse res = authService.login("admin", "pass");

        assertNotNull(res.getToken());
        assertNotNull(res.getUser());
        UserInfoVo user = res.getUser();
        assertEquals(USER_ID, user.getUserId());
        assertEquals("admin", user.getUsername());
        assertEquals("张三", user.getName());
        assertEquals("研发部", user.getDeptName());
        assertEquals("后端开发", user.getPostName());
        assertEquals(List.of("ADMIN"), user.getRoles());
        verify(userAccountMapper).update(any(), any());
    }

    @Test
    void getCurrentUser_nullOrBlank_returnsNull() {
        assertNull(authService.getCurrentUser(null));
        assertNull(authService.getCurrentUser(""));
        assertNull(authService.getCurrentUser("   "));
    }

    @Test
    void getCurrentUser_unknownToken_returnsNull() {
        assertNull(authService.getCurrentUser("unknown-token"));
    }

    @Test
    void getCurrentUser_afterLogin_returnsUser() {
        SysUserAccount account = new SysUserAccount();
        account.setId(USER_ID);
        account.setUsername("admin");
        account.setEmployeeId(EMPLOYEE_ID);
        when(userAccountMapper.selectOne(any())).thenReturn(account);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userAccountMapper.selectById(USER_ID)).thenReturn(account);

        SysEmployee employee = new SysEmployee();
        employee.setId(EMPLOYEE_ID);
        employee.setName("李四");
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);
        when(userRoleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of("STAFF"));

        authService.login("admin", "pass");
        when(tokenStore.get(anyString())).thenReturn(USER_ID);
        when(userInfoCache.get(eq(USER_ID), any(Supplier.class))).thenAnswer(inv -> inv.getArgument(1, Supplier.class).get());

        UserInfoVo current = authService.getCurrentUser("any-token");
        assertNotNull(current);
        assertEquals(USER_ID, current.getUserId());
        assertEquals("admin", current.getUsername());
        assertEquals("李四", current.getName());
    }

    @Test
    void logout_removesToken() {
        SysUserAccount account = new SysUserAccount();
        account.setId(USER_ID);
        account.setUsername("u");
        account.setPasswordHash("h");
        account.setStatus(1);
        account.setEmployeeId(EMPLOYEE_ID);
        when(userAccountMapper.selectOne(any())).thenReturn(account);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userAccountMapper.selectById(USER_ID)).thenReturn(account);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(new SysEmployee());
        when(userRoleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of());

        authService.login("u", "p");
        when(tokenStore.get(anyString())).thenReturn(USER_ID);
        when(userInfoCache.get(eq(USER_ID), any(Supplier.class))).thenAnswer(inv -> inv.getArgument(1, Supplier.class).get());
        assertNotNull(authService.getCurrentUser("t"));

        when(tokenStore.get(anyString())).thenReturn(null);
        authService.logout("t");
        assertNull(authService.getCurrentUser("t"));
    }
}
