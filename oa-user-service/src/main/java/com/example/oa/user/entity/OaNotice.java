package com.example.oa.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告表 oa_notice
 */
@Data
@TableName("oa_notice")
public class OaNotice {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long publisherUserId;
    private Integer isTop;
    private String status;
    private LocalDateTime publishAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
