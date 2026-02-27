package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件表 oa_attachment：MinIO 存储路径 + 元数据。
 */
@Data
@TableName("oa_attachment")
public class OaAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务类型：leave / expense 等 */
    private String businessType;
    /** 业务主键（提交后可回写，如 leave_apply.id） */
    private String businessId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileContentType;
    private Long uploadUserId;
    private LocalDateTime createdAt;
}
