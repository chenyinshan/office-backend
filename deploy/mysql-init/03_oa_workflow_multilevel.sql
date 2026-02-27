-- 多级审批：为已有库增加第二级节点 hr（人事审批）
-- 适用：已执行过 02_oa.sql 的库，将 start -> dept_leader -> end 改为 start -> dept_leader -> hr -> end
-- 可重复执行：UPDATE 幂等，INSERT 使用 IGNORE 避免重复插入
USE `oa`;

-- 部门主管通过后进入人事审批
UPDATE `wf_node` SET `next_node_keys` = '["hr"]' WHERE `definition_id` IN (1, 2) AND `node_key` = 'dept_leader';

-- 新增人事审批节点（若已存在则忽略）
INSERT IGNORE INTO `wf_node` (`definition_id`, `node_key`, `node_name`, `node_type`, `approver_type`, `approver_config`, `next_node_keys`, `sort_order`)
VALUES
(1, 'hr', '人事审批', 'approval', 'assignee', '{"assignee_user_id":1}', '["end"]', 2),
(2, 'hr', '人事审批', 'approval', 'assignee', '{"assignee_user_id":1}', '["end"]', 2);

-- 将原 end 节点 sort_order 改为 3（可选）
UPDATE `wf_node` SET `sort_order` = 3 WHERE `definition_id` IN (1, 2) AND `node_key` = 'end';
