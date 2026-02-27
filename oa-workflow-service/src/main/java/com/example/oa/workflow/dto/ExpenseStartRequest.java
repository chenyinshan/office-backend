package com.example.oa.workflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseStartRequest {

    @NotNull(message = "报销金额不能为空")
    @DecimalMin("0")
    private BigDecimal totalAmount;
    private String expenseType;
    private String description;
    /** 附件 id 列表，逗号分隔 */
    private java.util.List<Long> attachmentIds;
}
