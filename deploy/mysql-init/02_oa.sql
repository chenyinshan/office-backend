-- OA 办公管理系统库表初始化
-- 执行顺序在 01_xxl_job.sql 之后（按文件名排序）
-- 使用：docker compose 首次启动时自动执行 mysql-init 目录下脚本

CREATE DATABASE IF NOT EXISTS `oa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `oa`;

SET NAMES utf8mb4;

-- ===================== 组织与人员 =====================

-- 部门
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父部门 id，0 表示根',
  `dept_name` varchar(64) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(32) DEFAULT NULL COMMENT '部门编码',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `leader_employee_id` bigint DEFAULT NULL COMMENT '负责人员工 id',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 停用 1 正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 岗位
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_code` varchar(32) NOT NULL COMMENT '岗位编码',
  `post_name` varchar(64) NOT NULL COMMENT '岗位名称',
  `sort_order` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 停用 1 正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- 员工
CREATE TABLE `sys_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dept_id` bigint NOT NULL COMMENT '部门 id',
  `post_id` bigint DEFAULT NULL COMMENT '岗位 id',
  `employee_no` varchar(32) NOT NULL COMMENT '工号',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `gender` tinyint DEFAULT NULL COMMENT '0 未知 1 男 2 女',
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `hire_date` date DEFAULT NULL COMMENT '入职日期',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 离职 1 在职',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- 登录账号（与员工 1:1 或 0:1，未建账号的员工不能登录）
CREATE TABLE `sys_user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NOT NULL COMMENT '员工 id',
  `username` varchar(64) NOT NULL COMMENT '登录名',
  `password_hash` varchar(128) NOT NULL COMMENT '密码哈希',
  `salt` varchar(32) DEFAULT NULL COMMENT '盐',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 禁用 1 正常',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_employee_id` (`employee_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表';

-- 角色
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(32) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `data_scope` tinyint DEFAULT 1 COMMENT '数据范围：1 本人 2 本部门 3 本部门及子部门 4 全部',
  `status` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户-角色关联
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT 'sys_user_account.id',
  `role_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ===================== 审批流 =====================

-- 流程定义（如：请假流程、报销流程）
CREATE TABLE `wf_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `process_key` varchar(32) NOT NULL COMMENT '流程唯一键，如 leave、expense',
  `process_name` varchar(64) NOT NULL COMMENT '流程名称',
  `version` int NOT NULL DEFAULT 1,
  `form_schema` json DEFAULT NULL COMMENT '表单配置（前端用）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 停用 1 启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_key_version` (`process_key`, `version`),
  KEY `idx_process_key` (`process_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';

-- 流程节点定义（每个流程的节点：发起→审批→结束）
CREATE TABLE `wf_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `definition_id` bigint NOT NULL COMMENT 'wf_definition.id',
  `node_key` varchar(32) NOT NULL COMMENT '节点唯一键，如 start、dept_leader、end',
  `node_name` varchar(64) NOT NULL COMMENT '节点名称',
  `node_type` varchar(16) NOT NULL COMMENT 'start/approval/end',
  `approver_type` varchar(32) DEFAULT NULL COMMENT '审批人类型：role/dept_leader/assignee',
  `approver_config` json DEFAULT NULL COMMENT '审批人配置，如 role_code、层级等',
  `next_node_keys` json DEFAULT NULL COMMENT '下一节点 key 列表，支持分支',
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_definition_node` (`definition_id`, `node_key`),
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点定义表';

-- 流程实例（一次发起产生一条）
CREATE TABLE `wf_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `definition_id` bigint NOT NULL,
  `process_key` varchar(32) NOT NULL COMMENT '冗余，便于查询',
  `business_type` varchar(32) NOT NULL COMMENT '业务类型：leave/expense 等',
  `business_id` bigint DEFAULT NULL COMMENT '业务主键，如 leave_apply.id',
  `title` varchar(256) NOT NULL COMMENT '流程标题',
  `applicant_user_id` bigint NOT NULL COMMENT '发起人 sys_user_account.id',
  `applicant_employee_id` bigint NOT NULL COMMENT '发起人员工 id',
  `status` varchar(16) NOT NULL COMMENT 'draft/running/completed/rejected/cancelled',
  `current_node_key` varchar(32) DEFAULT NULL COMMENT '当前所在节点',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_key` (`process_key`),
  KEY `idx_applicant_user` (`applicant_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';

-- 待办任务（当前需要某人处理的节点）
CREATE TABLE `wf_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL COMMENT 'wf_instance.id',
  `node_key` varchar(32) NOT NULL COMMENT '当前节点',
  `assignee_user_id` bigint NOT NULL COMMENT '处理人 sys_user_account.id',
  `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/transferred',
  `result_comment` varchar(512) DEFAULT NULL COMMENT '审批意见',
  `acted_at` datetime DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_assignee` (`assignee_user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办任务表';

-- 审批操作记录（每次审批一条）
CREATE TABLE `wf_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT 'wf_task.id',
  `instance_id` bigint NOT NULL,
  `node_key` varchar(32) NOT NULL,
  `operator_user_id` bigint NOT NULL COMMENT '操作人',
  `action` varchar(16) NOT NULL COMMENT 'approve/reject/transfer',
  `comment` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批操作记录表';

-- ===================== 业务表单（与流程实例 1:1） =====================

-- 请假单
CREATE TABLE `leave_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL COMMENT 'wf_instance.id',
  `applicant_user_id` bigint NOT NULL,
  `leave_type` varchar(16) NOT NULL COMMENT 'annual/sick/personal 等',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `days` decimal(5,2) NOT NULL COMMENT '天数',
  `reason` varchar(512) DEFAULT NULL,
  `attachment_ids` varchar(256) DEFAULT NULL COMMENT '附件 id 逗号分隔',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instance_id` (`instance_id`),
  KEY `idx_applicant` (`applicant_user_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

-- 报销单
CREATE TABLE `expense_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `instance_id` bigint NOT NULL COMMENT 'wf_instance.id',
  `applicant_user_id` bigint NOT NULL,
  `total_amount` decimal(12,2) NOT NULL COMMENT '报销总金额',
  `expense_type` varchar(32) DEFAULT NULL COMMENT '差旅/办公等',
  `description` varchar(512) DEFAULT NULL,
  `attachment_ids` varchar(256) DEFAULT NULL COMMENT '发票等附件 id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instance_id` (`instance_id`),
  KEY `idx_applicant` (`applicant_user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销申请表';

-- ===================== 公告与通知 =====================

-- 公告
CREATE TABLE `oa_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(256) NOT NULL,
  `content` text COMMENT '正文',
  `publisher_user_id` bigint NOT NULL COMMENT '发布人',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '0 否 1 置顶',
  `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/published',
  `publish_at` datetime DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_at` (`publish_at`),
  KEY `idx_is_top` (`is_top`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- 公告阅读记录
CREATE TABLE `oa_notice_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notice_id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT 'sys_user_account.id',
  `read_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告阅读记录表';

-- 站内通知（审批通过/驳回、公告等）
CREATE TABLE `oa_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人 sys_user_account.id',
  `type` varchar(32) NOT NULL COMMENT 'workflow_approved/workflow_rejected/notice_published 等',
  `title` varchar(256) NOT NULL COMMENT '标题',
  `content` varchar(512) DEFAULT NULL COMMENT '摘要或详情',
  `business_type` varchar(32) DEFAULT NULL COMMENT 'workflow/notice',
  `business_id` varchar(64) DEFAULT NULL COMMENT '关联业务 id，如 instance_id、notice_id',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_read_created` (`user_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知表';

-- ===================== 附件 =====================

CREATE TABLE `oa_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_type` varchar(32) NOT NULL COMMENT 'leave/expense/notice 等',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务主键或组合键',
  `file_name` varchar(256) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(512) NOT NULL COMMENT '存储路径（MinIO key 或相对路径）',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '字节',
  `file_content_type` varchar(128) DEFAULT NULL,
  `upload_user_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_upload_user` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

-- ===================== 初始数据（可选） =====================

INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `sort_order`, `status`)
VALUES (1, 0, '总公司', 'ROOT', 0, 1);

INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `sort_order`, `status`)
VALUES (1, 'STAFF', '普通员工', 0, 1), (2, 'DEPT_LEADER', '部门主管', 1, 1), (3, 'ADMIN', '管理员', 2, 1);

INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `data_scope`, `status`)
VALUES (1, 'STAFF', '员工', 1, 1), (2, 'DEPT_LEADER', '部门主管', 3, 1), (3, 'ADMIN', '系统管理员', 4, 1);

-- 流程定义示例：请假流程
INSERT INTO `wf_definition` (`id`, `process_key`, `process_name`, `version`, `status`)
VALUES (1, 'leave', '请假流程', 1, 1);

-- 请假流程节点：start -> dept_leader -> hr -> end（多级审批）
INSERT INTO `wf_node` (`definition_id`, `node_key`, `node_name`, `node_type`, `approver_type`, `approver_config`, `next_node_keys`, `sort_order`)
VALUES
(1, 'start', '发起', 'start', NULL, NULL, '["dept_leader"]', 0),
(1, 'dept_leader', '部门主管审批', 'approval', 'dept_leader', '{}', '["hr"]', 1),
(1, 'hr', '人事审批', 'approval', 'assignee', '{"assignee_user_id":1}', '["end"]', 2),
(1, 'end', '结束', 'end', NULL, NULL, NULL, 3);

-- 报销流程定义（可选，与请假类似）
INSERT INTO `wf_definition` (`id`, `process_key`, `process_name`, `version`, `status`)
VALUES (2, 'expense', '报销流程', 1, 1);

INSERT INTO `wf_node` (`definition_id`, `node_key`, `node_name`, `node_type`, `approver_type`, `approver_config`, `next_node_keys`, `sort_order`)
VALUES
(2, 'start', '发起', 'start', NULL, NULL, '["dept_leader"]', 0),
(2, 'dept_leader', '部门主管审批', 'approval', 'dept_leader', '{}', '["hr"]', 1),
(2, 'hr', '人事审批', 'approval', 'assignee', '{"assignee_user_id":1}', '["end"]', 2),
(2, 'end', '结束', 'end', NULL, NULL, NULL, 3);

-- 测试账号：员工 E001 + 用户 admin / 123456（密码 BCrypt）
INSERT INTO `sys_employee` (`id`, `dept_id`, `post_id`, `employee_no`, `name`, `status`)
VALUES (1, 1, 1, 'E001', '管理员', 1);

INSERT INTO `sys_user_account` (`id`, `employee_id`, `username`, `password_hash`, `status`)
VALUES (1, 1, 'admin', '$2a$10$bxwtMk4Drj/oLWUFFK5f3ezmQodXh/Ii2cEJCOg/ayds9a79.f9Gq', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 3);

-- 主管账号：用于审批流 dept_leader 节点审批人，leader / 123456（与 admin 同哈希）
INSERT INTO `sys_employee` (`id`, `dept_id`, `post_id`, `employee_no`, `name`, `status`)
VALUES (2, 1, 2, 'E002', '部门主管', 1);

INSERT INTO `sys_user_account` (`id`, `employee_id`, `username`, `password_hash`, `status`)
VALUES (2, 2, 'leader', '$2a$10$bxwtMk4Drj/oLWUFFK5f3ezmQodXh/Ii2cEJCOg/ayds9a79.f9Gq', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 2);

-- 审批流节点配置：部门主管节点指定审批人为 user_id=2
UPDATE `wf_node` SET `approver_config` = '{"assignee_user_id":2}' WHERE `node_key` = 'dept_leader';
