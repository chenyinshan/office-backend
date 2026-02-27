package com.example.oa.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知列表/详情
 */
@Data
public class NotificationVo {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String businessType;
    private String businessId;
    private Integer isRead;
    private LocalDateTime createdAt;

    public static NotificationVo fromEntity(com.example.oa.user.entity.OaNotification n) {
        NotificationVo v = new NotificationVo();
        v.setId(n.getId());
        v.setUserId(n.getUserId());
        v.setType(n.getType());
        v.setTitle(n.getTitle());
        v.setContent(n.getContent());
        v.setBusinessType(n.getBusinessType());
        v.setBusinessId(n.getBusinessId());
        v.setIsRead(n.getIsRead());
        v.setCreatedAt(n.getCreatedAt());
        return v;
    }
}
