package com.example.oa.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.workflow.dto.ExpenseStatsVo;
import com.example.oa.workflow.dto.LeaveStatsVo;
import com.example.oa.workflow.entity.ExpenseApply;
import com.example.oa.workflow.entity.LeaveApply;
import com.example.oa.workflow.entity.WfInstance;
import com.example.oa.workflow.mapper.ExpenseApplyMapper;
import com.example.oa.workflow.mapper.LeaveApplyMapper;
import com.example.oa.workflow.mapper.WfInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表统计：请假、报销按时间范围与类型汇总（仅统计已完成的流程）。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final LeaveApplyMapper leaveApplyMapper;
    private final ExpenseApplyMapper expenseApplyMapper;
    private final WfInstanceMapper instanceMapper;

    /**
     * 请假统计：指定日期范围内的已完成请假，按类型汇总天数和笔数。
     * start/end 为空时统计全部。
     */
    public LeaveStatsVo leaveStats(LocalDate start, LocalDate end) {
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.plusDays(1).atStartOfDay();
        List<LeaveApply> list = leaveApplyMapper.selectList(
                Wrappers.<LeaveApply>lambdaQuery()
                        .ge(startTime != null, LeaveApply::getStartTime, startTime)
                        .lt(endTime != null, LeaveApply::getStartTime, endTime != null ? endTime : LocalDate.MAX.atTime(LocalTime.MAX)));
        if (list.isEmpty()) {
            LeaveStatsVo vo = new LeaveStatsVo();
            vo.setByType(Collections.emptyList());
            vo.setTotalDays(BigDecimal.ZERO);
            vo.setTotalCount(0L);
            return vo;
        }
        Set<Long> instanceIds = list.stream().map(LeaveApply::getInstanceId).collect(Collectors.toSet());
        List<WfInstance> instances = instanceMapper.selectBatchIds(instanceIds);
        Set<Long> completedIds = instances.stream()
                .filter(i -> "completed".equals(i.getStatus()))
                .map(WfInstance::getId)
                .collect(Collectors.toSet());
        List<LeaveApply> completed = list.stream()
                .filter(l -> completedIds.contains(l.getInstanceId()))
                .collect(Collectors.toList());
        Map<String, List<LeaveApply>> byType = completed.stream().collect(Collectors.groupingBy(l -> l.getLeaveType() != null ? l.getLeaveType() : ""));
        List<LeaveStatsVo.LeaveTypeItem> items = new ArrayList<>();
        BigDecimal totalDays = BigDecimal.ZERO;
        for (Map.Entry<String, List<LeaveApply>> e : byType.entrySet()) {
            LeaveStatsVo.LeaveTypeItem item = new LeaveStatsVo.LeaveTypeItem();
            item.setLeaveType(e.getKey());
            BigDecimal sum = e.getValue().stream().map(LeaveApply::getDays).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            item.setDays(sum);
            item.setCount(e.getValue().size());
            items.add(item);
            totalDays = totalDays.add(sum);
        }
        items.sort(Comparator.comparing(LeaveStatsVo.LeaveTypeItem::getLeaveType));
        LeaveStatsVo vo = new LeaveStatsVo();
        vo.setByType(items);
        vo.setTotalDays(totalDays);
        vo.setTotalCount(completed.size());
        return vo;
    }

    /**
     * 报销统计：指定日期范围内已完成的报销，按类型汇总金额和笔数。
     */
    public ExpenseStatsVo expenseStats(LocalDate start, LocalDate end) {
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.plusDays(1).atStartOfDay();
        List<ExpenseApply> list = expenseApplyMapper.selectList(
                Wrappers.<ExpenseApply>lambdaQuery()
                        .ge(startTime != null, ExpenseApply::getCreatedAt, startTime)
                        .lt(endTime != null, ExpenseApply::getCreatedAt, endTime != null ? endTime : LocalDateTime.MAX));
        if (list.isEmpty()) {
            ExpenseStatsVo vo = new ExpenseStatsVo();
            vo.setByType(Collections.emptyList());
            vo.setTotalAmount(BigDecimal.ZERO);
            vo.setTotalCount(0L);
            return vo;
        }
        Set<Long> instanceIds = list.stream().map(ExpenseApply::getInstanceId).collect(Collectors.toSet());
        List<WfInstance> instances = instanceMapper.selectBatchIds(instanceIds);
        Set<Long> completedIds = instances.stream()
                .filter(i -> "completed".equals(i.getStatus()))
                .map(WfInstance::getId)
                .collect(Collectors.toSet());
        List<ExpenseApply> completed = list.stream()
                .filter(e -> completedIds.contains(e.getInstanceId()))
                .collect(Collectors.toList());
        Map<String, List<ExpenseApply>> byType = completed.stream()
                .collect(Collectors.groupingBy(e -> e.getExpenseType() != null ? e.getExpenseType() : ""));
        List<ExpenseStatsVo.ExpenseTypeItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map.Entry<String, List<ExpenseApply>> e : byType.entrySet()) {
            ExpenseStatsVo.ExpenseTypeItem item = new ExpenseStatsVo.ExpenseTypeItem();
            item.setExpenseType(e.getKey());
            BigDecimal sum = e.getValue().stream().map(ExpenseApply::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            item.setAmount(sum);
            item.setCount(e.getValue().size());
            items.add(item);
            totalAmount = totalAmount.add(sum);
        }
        items.sort(Comparator.comparing(ExpenseStatsVo.ExpenseTypeItem::getExpenseType));
        ExpenseStatsVo vo = new ExpenseStatsVo();
        vo.setByType(items);
        vo.setTotalAmount(totalAmount);
        vo.setTotalCount(completed.size());
        return vo;
    }
}
