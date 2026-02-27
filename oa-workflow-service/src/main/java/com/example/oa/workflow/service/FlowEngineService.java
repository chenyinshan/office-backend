package com.example.oa.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.oa.common.BizException;
import com.example.oa.common.ResultCode;
import com.example.oa.workflow.dto.InstanceDetailVo;
import com.example.oa.workflow.dto.TaskDetailVo;
import com.example.oa.workflow.dto.TaskLogVo;
import com.example.oa.workflow.dto.ApprovalResultVo;
import com.example.oa.workflow.entity.*;
import com.example.oa.workflow.mapper.*;
import com.example.oa.workflow.util.WfJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程引擎：发起、推进、审批/驳回。
 */
@Service
@RequiredArgsConstructor
public class FlowEngineService {

    private final WfDefinitionMapper definitionMapper;
    private final WfNodeMapper nodeMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfTaskLogMapper taskLogMapper;
    private final LeaveApplyMapper leaveApplyMapper;
    private final ExpenseApplyMapper expenseApplyMapper;
    private final AttachmentService attachmentService;

    private static final String NODE_START = "start";
    private static final String NODE_END = "end";

    /** 发起请假流程 */
    @Transactional(rollbackFor = Exception.class)
    public WfInstance startLeaveProcess(Long applicantUserId, Long applicantEmployeeId,
                                       String leaveType, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                                       java.math.BigDecimal days, String reason, java.util.List<Long> attachmentIds) {
        WfDefinition def = getDefinitionByProcessKey("leave");
        String title = "请假申请 " + leaveType + " " + days + "天";

        WfInstance inst = startProcessWithoutBusiness(def, "leave", title, applicantUserId, applicantEmployeeId);

        LeaveApply leave = new LeaveApply();
        leave.setInstanceId(inst.getId());
        leave.setApplicantUserId(applicantUserId);
        leave.setLeaveType(leaveType);
        leave.setStartTime(startTime);
        leave.setEndTime(endTime);
        leave.setDays(days);
        leave.setReason(reason);
        leave.setAttachmentIds(attachmentIds == null || attachmentIds.isEmpty() ? null : attachmentIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        leaveApplyMapper.insert(leave);

        inst.setBusinessId(leave.getId());
        instanceMapper.updateById(inst);

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentService.linkToBusiness("leave", String.valueOf(leave.getId()), attachmentIds);
        }
        return inst;
    }

    /** 发起报销流程 */
    @Transactional(rollbackFor = Exception.class)
    public WfInstance startExpenseProcess(Long applicantUserId, Long applicantEmployeeId,
                                          java.math.BigDecimal totalAmount, String expenseType, String description,
                                          java.util.List<Long> attachmentIds) {
        WfDefinition def = getDefinitionByProcessKey("expense");
        String title = "报销申请 " + expenseType + " " + totalAmount + "元";

        WfInstance inst = startProcessWithoutBusiness(def, "expense", title, applicantUserId, applicantEmployeeId);

        ExpenseApply expense = new ExpenseApply();
        expense.setInstanceId(inst.getId());
        expense.setApplicantUserId(applicantUserId);
        expense.setTotalAmount(totalAmount);
        expense.setExpenseType(expenseType);
        expense.setDescription(description);
        expense.setAttachmentIds(attachmentIds == null || attachmentIds.isEmpty() ? null : attachmentIds.stream().map(Object::toString).collect(Collectors.joining(",")));
        expenseApplyMapper.insert(expense);

        inst.setBusinessId(expense.getId());
        instanceMapper.updateById(inst);

        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            attachmentService.linkToBusiness("expense", String.valueOf(expense.getId()), attachmentIds);
        }
        return inst;
    }

    /** 创建实例并生成第一个待办（业务表由调用方插入后回写 businessId） */
    private WfInstance startProcessWithoutBusiness(WfDefinition def, String businessType, String title,
                                    Long applicantUserId, Long applicantEmployeeId) {
        WfNode startNode = getNode(def.getId(), NODE_START);
        List<String> nextKeys = WfJsonUtil.parseNextNodeKeys(startNode.getNextNodeKeys());
        if (nextKeys.isEmpty()) {
            throw new BizException(ResultCode.WORKFLOW_DEFINITION_NOT_FOUND, "流程未配置下一节点");
        }
        String firstNodeKey = nextKeys.get(0);
        WfNode firstNode = getNode(def.getId(), firstNodeKey);

        WfInstance inst = new WfInstance();
        inst.setDefinitionId(def.getId());
        inst.setProcessKey(def.getProcessKey());
        inst.setBusinessType(businessType);
        inst.setBusinessId(null);
        inst.setTitle(title);
        inst.setApplicantUserId(applicantUserId);
        inst.setApplicantEmployeeId(applicantEmployeeId);
        inst.setStatus("running");
        inst.setCurrentNodeKey(firstNodeKey);
        instanceMapper.insert(inst);

        Long assigneeId = resolveAssignee(firstNode, inst);
        if (assigneeId == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "无法确定审批人，请检查流程节点配置 approver_config");
        }
        createTask(inst.getId(), firstNodeKey, assigneeId);

        return inst;
    }

    /** 我的待办（pending） */
    public List<WfTask> listPendingTasks(Long userId) {
        return taskMapper.selectList(
                Wrappers.<WfTask>lambdaQuery()
                        .eq(WfTask::getAssigneeUserId, userId)
                        .eq(WfTask::getStatus, "pending")
                        .orderByDesc(WfTask::getCreatedAt));
    }

    /** 我发起的流程 */
    public List<WfInstance> listMyInstances(Long userId) {
        return instanceMapper.selectList(
                Wrappers.<WfInstance>lambdaQuery()
                        .eq(WfInstance::getApplicantUserId, userId)
                        .orderByDesc(WfInstance::getCreatedAt));
    }

    /** 我的待办（带业务详情） */
    public List<TaskDetailVo> listPendingTasksWithDetail(Long userId) {
        List<WfTask> tasks = listPendingTasks(userId);
        List<TaskDetailVo> result = new ArrayList<>(tasks.size());
        for (WfTask task : tasks) {
            TaskDetailVo vo = toTaskDetailVo(task);
            if (vo != null) result.add(vo);
        }
        return result;
    }

    /** 我的任务（待办+已处理，可选状态筛选）；status 为空或 all 表示全部，pending/approved/rejected 筛选 */
    public List<TaskDetailVo> listMyTasksWithDetail(Long userId, String status) {
        var query = Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getAssigneeUserId, userId)
                .orderByDesc(WfTask::getCreatedAt);
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status.trim())) {
            query.eq(WfTask::getStatus, status.trim().toLowerCase());
        }
        List<WfTask> tasks = taskMapper.selectList(query);
        List<TaskDetailVo> result = new ArrayList<>(tasks.size());
        for (WfTask task : tasks) {
            TaskDetailVo vo = toTaskDetailVo(task);
            if (vo != null) result.add(vo);
        }
        return result;
    }

    /** 我发起的流程（带业务详情） */
    public List<InstanceDetailVo> listMyInstancesWithDetail(Long userId) {
        List<WfInstance> instances = listMyInstances(userId);
        List<InstanceDetailVo> result = new ArrayList<>(instances.size());
        for (WfInstance inst : instances) {
            result.add(toInstanceDetailVo(inst));
        }
        return result;
    }

    /** 单条实例详情（仅发起人可查） */
    public InstanceDetailVo getInstanceDetail(Long instanceId, Long userId) {
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new BizException(ResultCode.WORKFLOW_INSTANCE_NOT_FOUND);
        if (!inst.getApplicantUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return toInstanceDetailVo(inst);
    }

    /** 实例的审批记录（仅发起人可查） */
    public List<TaskLogVo> listInstanceTaskLogs(Long instanceId, Long userId) {
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) throw new BizException(ResultCode.WORKFLOW_INSTANCE_NOT_FOUND);
        if (!inst.getApplicantUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        List<WfTaskLog> logs = taskLogMapper.selectList(
                Wrappers.<WfTaskLog>lambdaQuery()
                        .eq(WfTaskLog::getInstanceId, instanceId)
                        .orderByAsc(WfTaskLog::getCreatedAt));
        List<TaskLogVo> result = new ArrayList<>(logs.size());
        for (WfTaskLog log : logs) {
            TaskLogVo vo = new TaskLogVo();
            vo.setId(log.getId());
            vo.setTaskId(log.getTaskId());
            vo.setInstanceId(log.getInstanceId());
            vo.setNodeKey(log.getNodeKey());
            vo.setOperatorUserId(log.getOperatorUserId());
            vo.setAction(log.getAction());
            vo.setComment(log.getComment());
            vo.setCreatedAt(log.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    private TaskDetailVo toTaskDetailVo(WfTask task) {
        WfInstance inst = instanceMapper.selectById(task.getInstanceId());
        if (inst == null) return null;
        TaskDetailVo vo = new TaskDetailVo();
        vo.setId(task.getId());
        vo.setInstanceId(task.getInstanceId());
        vo.setNodeKey(task.getNodeKey());
        vo.setAssigneeUserId(task.getAssigneeUserId());
        vo.setStatus(task.getStatus());
        vo.setResultComment(task.getResultComment());
        vo.setActedAt(task.getActedAt());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        vo.setTitle(inst.getTitle());
        vo.setBusinessType(inst.getBusinessType());
        try {
            WfNode node = getNode(inst.getDefinitionId(), task.getNodeKey());
            if (node != null) vo.setNodeName(node.getNodeName());
        } catch (Exception ignored) {}
        fillBusinessDetail(inst, vo);
        return vo;
    }

    private InstanceDetailVo toInstanceDetailVo(WfInstance inst) {
        InstanceDetailVo vo = new InstanceDetailVo();
        vo.setId(inst.getId());
        vo.setDefinitionId(inst.getDefinitionId());
        vo.setProcessKey(inst.getProcessKey());
        vo.setBusinessType(inst.getBusinessType());
        vo.setBusinessId(inst.getBusinessId());
        vo.setTitle(inst.getTitle());
        vo.setApplicantUserId(inst.getApplicantUserId());
        vo.setApplicantEmployeeId(inst.getApplicantEmployeeId());
        vo.setStatus(inst.getStatus());
        vo.setCurrentNodeKey(inst.getCurrentNodeKey());
        vo.setCreatedAt(inst.getCreatedAt());
        vo.setUpdatedAt(inst.getUpdatedAt());
        vo.setFinishedAt(inst.getFinishedAt());
        if ("running".equals(inst.getStatus()) && inst.getCurrentNodeKey() != null) {
            try {
                WfNode node = getNode(inst.getDefinitionId(), inst.getCurrentNodeKey());
                if (node != null) vo.setCurrentNodeName(node.getNodeName());
            } catch (Exception ignored) {}
        }
        if (inst.getBusinessId() != null && inst.getBusinessType() != null) {
            if ("leave".equals(inst.getBusinessType())) {
                LeaveApply leave = leaveApplyMapper.selectById(inst.getBusinessId());
                if (leave != null) {
                    vo.setLeaveType(leave.getLeaveType());
                    vo.setLeaveDays(leave.getDays());
                    vo.setLeaveReason(leave.getReason());
                    vo.setLeaveStartTime(leave.getStartTime());
                    vo.setLeaveEndTime(leave.getEndTime());
                }
            } else if ("expense".equals(inst.getBusinessType())) {
                ExpenseApply exp = expenseApplyMapper.selectById(inst.getBusinessId());
                if (exp != null) {
                    vo.setExpenseAmount(exp.getTotalAmount());
                    vo.setExpenseType(exp.getExpenseType());
                    vo.setExpenseDescription(exp.getDescription());
                }
            }
        }
        return vo;
    }

    private void fillBusinessDetail(WfInstance inst, TaskDetailVo vo) {
        if (inst.getBusinessId() == null || inst.getBusinessType() == null) return;
        if ("leave".equals(inst.getBusinessType())) {
            LeaveApply leave = leaveApplyMapper.selectById(inst.getBusinessId());
            if (leave != null) {
                vo.setLeaveType(leave.getLeaveType());
                vo.setLeaveDays(leave.getDays());
                vo.setLeaveReason(leave.getReason());
                vo.setLeaveStartTime(leave.getStartTime());
                vo.setLeaveEndTime(leave.getEndTime());
            }
        } else if ("expense".equals(inst.getBusinessType())) {
            ExpenseApply exp = expenseApplyMapper.selectById(inst.getBusinessId());
            if (exp != null) {
                vo.setExpenseAmount(exp.getTotalAmount());
                vo.setExpenseType(exp.getExpenseType());
                vo.setExpenseDescription(exp.getDescription());
            }
        }
    }

    /** 通过 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalResultVo approve(Long taskId, Long operatorUserId, String comment) {
        return doAct(taskId, operatorUserId, comment, "approve", "approved");
    }

    /** 驳回 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalResultVo reject(Long taskId, Long operatorUserId, String comment) {
        return doAct(taskId, operatorUserId, comment, "reject", "rejected");
    }

    private ApprovalResultVo doAct(Long taskId, Long operatorUserId, String comment, String action, String taskStatus) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BizException(ResultCode.WORKFLOW_TASK_NOT_FOUND);
        if (!"pending".equals(task.getStatus())) throw new BizException(ResultCode.WORKFLOW_NOT_PENDING);
        if (!task.getAssigneeUserId().equals(operatorUserId)) throw new BizException(ResultCode.FORBIDDEN);

        WfInstance inst = instanceMapper.selectById(task.getInstanceId());
        if (inst == null) throw new BizException(ResultCode.WORKFLOW_INSTANCE_NOT_FOUND);
        if (!"running".equals(inst.getStatus())) throw new BizException(ResultCode.WORKFLOW_ALREADY_FINISHED);

        ApprovalResultVo result = new ApprovalResultVo(
                inst.getApplicantUserId(),
                inst.getId(),
                inst.getBusinessType(),
                inst.getTitle(),
                "approved".equals(taskStatus));

        task.setStatus(taskStatus);
        task.setResultComment(comment);
        task.setActedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        WfTaskLog log = new WfTaskLog();
        log.setTaskId(taskId);
        log.setInstanceId(inst.getId());
        log.setNodeKey(task.getNodeKey());
        log.setOperatorUserId(operatorUserId);
        log.setAction(action);
        log.setComment(comment);
        taskLogMapper.insert(log);

        if ("rejected".equals(taskStatus)) {
            inst.setStatus("rejected");
            inst.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);
            return result;
        }

        List<String> nextKeys = WfJsonUtil.parseNextNodeKeys(getNode(inst.getDefinitionId(), task.getNodeKey()).getNextNodeKeys());
        if (nextKeys.isEmpty() || NODE_END.equals(nextKeys.get(0))) {
            inst.setStatus("completed");
            inst.setCurrentNodeKey(NODE_END);
            inst.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(inst);
            return result;
        }

        String nextKey = nextKeys.get(0);
        WfNode nextNode = getNode(inst.getDefinitionId(), nextKey);
        Long nextAssignee = resolveAssignee(nextNode, inst);
        inst.setCurrentNodeKey(nextKey);
        instanceMapper.updateById(inst);
        if (nextAssignee != null) {
            createTask(inst.getId(), nextKey, nextAssignee);
        }
        return result;
    }

    private WfDefinition getDefinitionByProcessKey(String processKey) {
        WfDefinition def = definitionMapper.selectOne(
                Wrappers.<WfDefinition>lambdaQuery()
                        .eq(WfDefinition::getProcessKey, processKey)
                        .eq(WfDefinition::getStatus, 1)
                        .last("LIMIT 1"));
        if (def == null) throw new BizException(ResultCode.WORKFLOW_DEFINITION_NOT_FOUND);
        return def;
    }

    private WfNode getNode(Long definitionId, String nodeKey) {
        WfNode node = nodeMapper.selectOne(
                Wrappers.<WfNode>lambdaQuery()
                        .eq(WfNode::getDefinitionId, definitionId)
                        .eq(WfNode::getNodeKey, nodeKey)
                        .last("LIMIT 1"));
        if (node == null) throw new BizException(ResultCode.WORKFLOW_DEFINITION_NOT_FOUND, "节点不存在: " + nodeKey);
        return node;
    }

    private Long resolveAssignee(WfNode node, WfInstance inst) {
        return WfJsonUtil.parseAssigneeUserId(node.getApproverConfig());
    }

    private void createTask(Long instanceId, String nodeKey, Long assigneeUserId) {
        WfTask t = new WfTask();
        t.setInstanceId(instanceId);
        t.setNodeKey(nodeKey);
        t.setAssigneeUserId(assigneeUserId);
        t.setStatus("pending");
        taskMapper.insert(t);
    }
}
