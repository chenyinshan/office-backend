package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知 oa_notification
 */
@Data
@TableName("oa_notification")
public class OaNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String businessType;
    private String businessId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
