package com.example.oa.user.controller;

import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.user.dto.LoginRequest;
import com.example.oa.user.dto.LoginResponse;
import com.example.oa.user.dto.UserInfoVo;
import com.example.oa.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录、登出、当前用户
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String HEADER_TOKEN = "Authorization";

    /** 从 Header 取 token：支持 "Bearer xxx" 或 直接 "xxx" */
    private static String resolveToken(HttpServletRequest request) {
        String v = request.getHeader(HEADER_TOKEN);
        if (v == null || v.isBlank()) {
            return null;
        }
        v = v.trim();
        if (v.startsWith("Bearer ")) {
            return v.substring(7).trim();
        }
        return v;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        LoginResponse resp = authService.login(req.getUsername(), req.getPassword());
        return Result.ok(resp);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserInfoVo> me(HttpServletRequest request) {
        String token = resolveToken(request);
        UserInfoVo user = authService.getCurrentUser(token);
        if (user == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return Result.ok(user);
    }
}
