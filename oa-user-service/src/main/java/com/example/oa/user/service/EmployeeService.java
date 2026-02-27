package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.user.entity.SysEmployee;
import com.example.oa.user.mapper.SysEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工：列表（按部门）、增删改。
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final SysEmployeeMapper employeeMapper;

    public List<SysEmployee> list(Long deptId) {
        if (deptId != null) {
            return employeeMapper.selectList(
                    Wrappers.<SysEmployee>lambdaQuery()
                            .eq(SysEmployee::getDeptId, deptId)
                            .orderByAsc(SysEmployee::getEmployeeNo));
        }
        // 全部员工按 id 倒序，新增加的排前面便于看到
        return employeeMapper.selectList(
                Wrappers.<SysEmployee>lambdaQuery()
                        .orderByDesc(SysEmployee::getId));
    }

    public SysEmployee getById(Long id) {
        return employeeMapper.selectById(id);
    }

    public SysEmployee create(Long deptId, Long postId, String employeeNo, String name,
                              Integer gender, String phone, String email, LocalDate hireDate) {
        SysEmployee e = new SysEmployee();
        e.setDeptId(deptId);
        e.setPostId(postId);
        e.setEmployeeNo(employeeNo);
        e.setName(name);
        e.setGender(gender);
        e.setPhone(phone);
        e.setEmail(email);
        e.setHireDate(hireDate);
        e.setStatus(1);
        employeeMapper.insert(e);
        return e;
    }

    public void update(Long id, Long deptId, Long postId, String employeeNo, String name,
                       Integer gender, String phone, String email, LocalDate hireDate, Integer status) {
        SysEmployee e = employeeMapper.selectById(id);
        if (e == null) return;
        if (deptId != null) e.setDeptId(deptId);
        if (postId != null) e.setPostId(postId);
        if (employeeNo != null) e.setEmployeeNo(employeeNo);
        if (name != null) e.setName(name);
        if (gender != null) e.setGender(gender);
        if (phone != null) e.setPhone(phone);
        if (email != null) e.setEmail(email);
        if (hireDate != null) e.setHireDate(hireDate);
        if (status != null) e.setStatus(status);
        employeeMapper.updateById(e);
    }

    public void delete(Long id) {
        employeeMapper.deleteById(id);
    }
}
