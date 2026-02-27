package com.example.oa.workflow.controller;

import com.example.oa.common.Result;
import com.example.oa.common.ResultCode;
import com.example.oa.common.BizException;
import com.example.oa.workflow.dto.ExpenseStartRequest;
import com.example.oa.workflow.dto.ExpenseStatsVo;
import com.example.oa.workflow.dto.InstanceDetailVo;
import com.example.oa.workflow.dto.LeaveStartRequest;
import com.example.oa.workflow.dto.LeaveStatsVo;
import com.example.oa.workflow.dto.TaskActRequest;
import com.example.oa.workflow.dto.TaskLogVo;
import com.example.oa.workflow.dto.ApprovalResultVo;
import com.example.oa.workflow.dto.TaskDetailVo;
import com.example.oa.workflow.entity.WfInstance;
import com.example.oa.workflow.service.FlowEngineService;
import com.example.oa.workflow.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 审批流接口。当前用户通过请求头 X-User-Id 传递（与 user-service 的 token 对应，由网关或前端解析后填入）。
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final FlowEngineService flowEngineService;
    private final ReportService reportService;

    private Long getCurrentUserId(HttpServletRequest request) {
        String v = request.getHeader(HEADER_USER_ID);
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        Long id = getCurrentUserId(request);
        if (id == null) throw new BizException(ResultCode.UNAUTHORIZED);
        return id;
    }

    /** 发起请假（需 X-User-Id 和 X-Employee-Id，暂无网关时前端或 Postman 传） */
    @PostMapping("/leave/start")
    public Result<WfInstance> startLeave(@RequestBody @Valid LeaveStartRequest req, HttpServletRequest request) {
        Long userId = requireUserId(request);
        Long employeeId = getEmployeeId(request);
        WfInstance inst = flowEngineService.startLeaveProcess(
                userId, employeeId != null ? employeeId : userId,
                req.getLeaveType(), req.getStartTime(), req.getEndTime(), req.getDays(), req.getReason(), req.getAttachmentIds());
        return Result.ok(inst);
    }

    /** 发起报销 */
    @PostMapping("/expense/start")
    public Result<WfInstance> startExpense(@RequestBody @Valid ExpenseStartRequest req, HttpServletRequest request) {
        Long userId = requireUserId(request);
        Long employeeId = getEmployeeId(request);
        WfInstance inst = flowEngineService.startExpenseProcess(
                userId, employeeId != null ? employeeId : userId,
                req.getTotalAmount(), req.getExpenseType(), req.getDescription(), req.getAttachmentIds());
        return Result.ok(inst);
    }

    /** 我的待办（含业务详情：请假类型/天数、报销金额等） */
    @GetMapping("/tasks/pending")
    public Result<List<TaskDetailVo>> pendingTasks(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(flowEngineService.listPendingTasksWithDetail(userId));
    }

    /** 我的任务（待办+已处理），可选 ?status=pending|approved|rejected|all */
    @GetMapping("/tasks/my")
    public Result<List<TaskDetailVo>> myTasks(
            @RequestParam(name = "status", required = false) String status,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(flowEngineService.listMyTasksWithDetail(userId, status));
    }

    /** 我发起的流程（含业务详情） */
    @GetMapping("/instances/my")
    public Result<List<InstanceDetailVo>> myInstances(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(flowEngineService.listMyInstancesWithDetail(userId));
    }

    /** 流程实例详情（仅发起人可查） */
    @GetMapping("/instances/{id}")
    public Result<InstanceDetailVo> getInstance(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(flowEngineService.getInstanceDetail(id, userId));
    }

    /** 流程实例的审批记录（仅发起人可查） */
    @GetMapping("/instances/{id}/logs")
    public Result<List<TaskLogVo>> getInstanceLogs(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return Result.ok(flowEngineService.listInstanceTaskLogs(id, userId));
    }

    /** 请假统计（可选 startDate/endDate 格式 yyyy-MM-dd，仅统计已完成的申请） */
    @GetMapping("/stats/leave")
    public Result<LeaveStatsVo> leaveStats(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletRequest request) {
        requireUserId(request);
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return Result.ok(reportService.leaveStats(start, end));
    }

    /** 报销统计（可选 startDate/endDate 格式 yyyy-MM-dd，仅统计已完成的申请） */
    @GetMapping("/stats/expense")
    public Result<ExpenseStatsVo> expenseStats(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            HttpServletRequest request) {
        requireUserId(request);
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return Result.ok(reportService.expenseStats(start, end));
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    /** 通过 */
    @PostMapping("/tasks/{taskId}/approve")
    public Result<ApprovalResultVo> approve(@PathVariable("taskId") Long taskId, @RequestBody(required = false) TaskActRequest body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        ApprovalResultVo result = flowEngineService.approve(taskId, userId, body != null ? body.getComment() : null);
        return Result.ok(result);
    }

    /** 驳回 */
    @PostMapping("/tasks/{taskId}/reject")
    public Result<ApprovalResultVo> reject(@PathVariable("taskId") Long taskId, @RequestBody(required = false) TaskActRequest body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        ApprovalResultVo result = flowEngineService.reject(taskId, userId, body != null ? body.getComment() : null);
        return Result.ok(result);
    }

    private Long getEmployeeId(HttpServletRequest request) {
        String v = request.getHeader("X-Employee-Id");
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
