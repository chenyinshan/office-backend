package com.example.oa.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告列表/详情 DTO
 */
@Data
public class NoticeVo {

    private Long id;
    private String title;
    private String content;
    private Long publisherUserId;
    private Integer isTop;
    private String status;
    private LocalDateTime publishAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 当前用户是否已读 */
    private Boolean read;

    public static NoticeVo fromEntity(com.example.oa.user.entity.OaNotice n, Boolean read) {
        NoticeVo v = new NoticeVo();
        v.setId(n.getId());
        v.setTitle(n.getTitle());
        v.setContent(n.getContent());
        v.setPublisherUserId(n.getPublisherUserId());
        v.setIsTop(n.getIsTop());
        v.setStatus(n.getStatus());
        v.setPublishAt(n.getPublishAt());
        v.setCreatedAt(n.getCreatedAt());
        v.setUpdatedAt(n.getUpdatedAt());
        v.setRead(read);
        return v;
    }
}
