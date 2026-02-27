package com.example.oa.workflow.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 请假统计：按类型汇总天数与笔数。
 */
@Data
public class LeaveStatsVo {

    /** 按请假类型汇总 */
    private List<LeaveTypeItem> byType;
    /** 总天数（仅已完成的申请） */
    private BigDecimal totalDays;
    /** 总笔数（仅已完成的申请） */
    private long totalCount;

    @Data
    public static class LeaveTypeItem {
        private String leaveType;
        private BigDecimal days;
        private long count;
    }
}
