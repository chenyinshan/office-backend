package com.example.oa.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("expense_apply")
public class ExpenseApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long applicantUserId;
    private BigDecimal totalAmount;
    private String expenseType;
    private String description;
    private String attachmentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
