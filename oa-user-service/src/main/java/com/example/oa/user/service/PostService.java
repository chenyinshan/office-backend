package com.example.oa.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.user.entity.SysPost;
import com.example.oa.user.mapper.SysPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位：列表、增删改。
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final SysPostMapper postMapper;

    public List<SysPost> list() {
        return postMapper.selectList(
                Wrappers.<SysPost>lambdaQuery()
                        .orderByAsc(SysPost::getSortOrder)
                        .orderByAsc(SysPost::getId));
    }

    public SysPost getById(Long id) {
        return postMapper.selectById(id);
    }

    public SysPost create(String postCode, String postName, Integer sortOrder) {
        SysPost p = new SysPost();
        p.setPostCode(postCode);
        p.setPostName(postName);
        p.setSortOrder(sortOrder != null ? sortOrder : 0);
        p.setStatus(1);
        postMapper.insert(p);
        return p;
    }

    public void update(Long id, String postCode, String postName, Integer sortOrder, Integer status) {
        SysPost p = postMapper.selectById(id);
        if (p == null) return;
        if (postCode != null) p.setPostCode(postCode);
        if (postName != null) p.setPostName(postName);
        if (sortOrder != null) p.setSortOrder(sortOrder);
        if (status != null) p.setStatus(status);
        postMapper.updateById(p);
    }

    public void delete(Long id) {
        postMapper.deleteById(id);
    }
}
