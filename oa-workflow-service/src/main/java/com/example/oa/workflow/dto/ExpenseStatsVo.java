package com.example.oa.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报销统计：按类型汇总金额与笔数。
 */
@Data
public class ExpenseStatsVo {

    /** 按报销类型汇总 */
    private List<ExpenseTypeItem> byType;
    /** 总金额（仅已完成的申请） */
    private BigDecimal totalAmount;
    /** 总笔数（仅已完成的申请） */
    private long totalCount;

    @Data
    public static class ExpenseTypeItem {
        private String expenseType;
        private BigDecimal amount;
        private long count;
    }
}
