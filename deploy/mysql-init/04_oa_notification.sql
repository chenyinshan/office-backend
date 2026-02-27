-- 站内通知表（已执行过 02_oa.sql 的库可单独执行）
USE `oa`;

CREATE TABLE IF NOT EXISTS `oa_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人 sys_user_account.id',
  `type` varchar(32) NOT NULL COMMENT 'workflow_approved/workflow_rejected/notice_published 等',
  `title` varchar(256) NOT NULL COMMENT '标题',
  `content` varchar(512) DEFAULT NULL COMMENT '摘要或详情',
  `business_type` varchar(32) DEFAULT NULL COMMENT 'workflow/notice',
  `business_id` varchar(64) DEFAULT NULL COMMENT '关联业务 id',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_read_created` (`user_id`, `is_read`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知表';
