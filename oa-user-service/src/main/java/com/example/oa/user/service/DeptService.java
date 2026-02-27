package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.user.entity.SysDept;
import com.example.oa.user.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门：树形列表、增删改。
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;

    /** 树形列表（仅两级：根 + 子，按 sort_order、id 排序） */
    public List<DeptTreeNode> listTree() {
        List<SysDept> all = deptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery()
                        .orderByAsc(SysDept::getSortOrder)
                        .orderByAsc(SysDept::getId));
        List<DeptTreeNode> roots = all.stream()
                .filter(d -> d.getParentId() == null || d.getParentId() == 0)
                .map(DeptTreeNode::from)
                .collect(Collectors.toList());
        for (DeptTreeNode root : roots) {
            root.setChildren(all.stream()
                    .filter(d -> root.getId().equals(d.getParentId()))
                    .map(DeptTreeNode::from)
                    .collect(Collectors.toList()));
        }
        return roots;
    }

    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    public SysDept create(Long parentId, String deptName, String deptCode, Integer sortOrder) {
        SysDept d = new SysDept();
        d.setParentId(parentId != null ? parentId : 0L);
        d.setDeptName(deptName);
        d.setDeptCode(deptCode);
        d.setSortOrder(sortOrder != null ? sortOrder : 0);
        d.setStatus(1);
        deptMapper.insert(d);
        return d;
    }

    public void update(Long id, String deptName, String deptCode, Integer sortOrder, Integer status) {
        SysDept d = deptMapper.selectById(id);
        if (d == null) return;
        if (deptName != null) d.setDeptName(deptName);
        if (deptCode != null) d.setDeptCode(deptCode);
        if (sortOrder != null) d.setSortOrder(sortOrder);
        if (status != null) d.setStatus(status);
        deptMapper.updateById(d);
    }

    public void delete(Long id) {
        deptMapper.deleteById(id);
    }

    @lombok.Data
    public static class DeptTreeNode {
        private Long id;
        private Long parentId;
        private String deptName;
        private String deptCode;
        private Integer sortOrder;
        private Integer status;
        private List<DeptTreeNode> children;

        static DeptTreeNode from(SysDept d) {
            DeptTreeNode n = new DeptTreeNode();
            n.setId(d.getId());
            n.setParentId(d.getParentId());
            n.setDeptName(d.getDeptName());
            n.setDeptCode(d.getDeptCode());
            n.setSortOrder(d.getSortOrder());
            n.setStatus(d.getStatus());
            return n;
        }
    }
}
