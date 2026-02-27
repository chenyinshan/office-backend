package com.example.oa.user.controller;

import com.example.oa.common.BizException;
import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.user.entity.SysDept;
import com.example.oa.user.entity.SysEmployee;
import com.example.oa.user.entity.SysPost;
import com.example.oa.user.service.DeptService;
import com.example.oa.user.service.EmployeeService;
import com.example.oa.user.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 组织管理：部门树、岗位、员工。需 X-User-Id（由 portal 转发时填入）。
 */
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final DeptService deptService;
    private final PostService postService;
    private final EmployeeService employeeService;

    private Long requireUserId(HttpServletRequest request) {
        String v = request.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) throw new BizException(ResultCode.UNAUTHORIZED);
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    // ---------- 部门 ----------
    @GetMapping("/depts/tree")
    public Result<List<DeptService.DeptTreeNode>> deptTree(HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(deptService.listTree());
    }

    @GetMapping("/depts/{id}")
    public Result<SysDept> deptById(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(deptService.getById(id));
    }

    @PostMapping("/depts")
    public Result<SysDept> deptCreate(@RequestBody DeptBody body, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(deptService.create(body.getParentId(), body.getDeptName(), body.getDeptCode(), body.getSortOrder()));
    }

    @PutMapping("/depts/{id}")
    public Result<Void> deptUpdate(@PathVariable("id") Long id, @RequestBody DeptBody body, HttpServletRequest request) {
        requireUserId(request);
        deptService.update(id, body.getDeptName(), body.getDeptCode(), body.getSortOrder(), body.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/depts/{id}")
    public Result<Void> deptDelete(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        deptService.delete(id);
        return Result.ok();
    }

    // ---------- 岗位 ----------
    @GetMapping("/posts")
    public Result<List<SysPost>> postList(HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(postService.list());
    }

    @GetMapping("/posts/{id}")
    public Result<SysPost> postById(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(postService.getById(id));
    }

    @PostMapping("/posts")
    public Result<SysPost> postCreate(@RequestBody PostBody body, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(postService.create(body.getPostCode(), body.getPostName(), body.getSortOrder()));
    }

    @PutMapping("/posts/{id}")
    public Result<Void> postUpdate(@PathVariable("id") Long id, @RequestBody PostBody body, HttpServletRequest request) {
        requireUserId(request);
        postService.update(id, body.getPostCode(), body.getPostName(), body.getSortOrder(), body.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> postDelete(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        postService.delete(id);
        return Result.ok();
    }

    // ---------- 员工 ----------
    @GetMapping("/employees")
    public Result<List<SysEmployee>> employeeList(@RequestParam(name = "deptId", required = false) Long deptId, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(employeeService.list(deptId));
    }

    @GetMapping("/employees/{id}")
    public Result<SysEmployee> employeeById(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(employeeService.getById(id));
    }

    @PostMapping("/employees")
    public Result<SysEmployee> employeeCreate(@RequestBody EmployeeBody body, HttpServletRequest request) {
        requireUserId(request);
        return Result.ok(employeeService.create(
                body.getDeptId(), body.getPostId(), body.getEmployeeNo(), body.getName(),
                body.getGender(), body.getPhone(), body.getEmail(), body.getHireDate()));
    }

    @PutMapping("/employees/{id}")
    public Result<Void> employeeUpdate(@PathVariable("id") Long id, @RequestBody EmployeeBody body, HttpServletRequest request) {
        requireUserId(request);
        employeeService.update(id, body.getDeptId(), body.getPostId(), body.getEmployeeNo(), body.getName(),
                body.getGender(), body.getPhone(), body.getEmail(), body.getHireDate(), body.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/employees/{id}")
    public Result<Void> employeeDelete(@PathVariable("id") Long id, HttpServletRequest request) {
        requireUserId(request);
        employeeService.delete(id);
        return Result.ok();
    }

    @lombok.Data
    public static class DeptBody {
        private Long parentId;
        private String deptName;
        private String deptCode;
        private Integer sortOrder;
        private Integer status;
    }

    @lombok.Data
    public static class PostBody {
        private String postCode;
        private String postName;
        private Integer sortOrder;
        private Integer status;
    }

    @lombok.Data
    public static class EmployeeBody {
        private Long deptId;
        private Long postId;
        private String employeeNo;
        private String name;
        private Integer gender;
        private String phone;
        private String email;
        private LocalDate hireDate;
        private Integer status;
    }
}
