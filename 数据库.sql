-- 广告设计公司客户对接平台数据库初始化脚本
-- MySQL 8.0
-- 设计目标：1.0 可直接使用，并为 2.0/3.0 的文件上传、阶段确认、已读未读、操作审计预留基础表结构。
-- 默认演示账号：
-- 客户：customer@163.com / 123456
-- 设计师：designer@example.com / 123456

CREATE DATABASE IF NOT EXISTS `advertisement_design`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `advertisement_design`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `consultant_human_message`;
DROP TABLE IF EXISTS `consultant_intake`;
DROP TABLE IF EXISTS `designer_profile`;
DROP TABLE IF EXISTS `conversation_read_state`;
DROP TABLE IF EXISTS `message_file`;
DROP TABLE IF EXISTS `project_file`;
DROP TABLE IF EXISTS `stage_action`;
DROP TABLE IF EXISTS `project_stage`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `conversation`;
DROP TABLE IF EXISTS `project`;
DROP TABLE IF EXISTS `portfolio_case`;
DROP TABLE IF EXISTS `file_asset`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email` VARCHAR(128) NOT NULL COMMENT '邮箱账号',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号，后续短信验证码登录预留',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `role` VARCHAR(32) NOT NULL COMMENT '角色：CUSTOMER / DESIGNER',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED / DISABLED',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_status` (`status`),
  CONSTRAINT `chk_user_role` CHECK (`role` IN ('CUSTOMER', 'DESIGNER')),
  CONSTRAINT `chk_user_status` CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `designer_profile` (
  `designer_id` BIGINT NOT NULL COMMENT '设计师用户 ID',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否参与自动匹配',
  `online` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在线',
  `specialties` JSON DEFAULT NULL COMMENT '专业方向列表',
  PRIMARY KEY (`designer_id`),
  CONSTRAINT `fk_designer_profile_user` FOREIGN KEY (`designer_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_designer_profile_enabled` CHECK (`enabled` IN (0, 1)),
  CONSTRAINT `chk_designer_profile_online` CHECK (`online` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询设计师匹配档案表';

CREATE TABLE `consultant_intake` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id` BIGINT NOT NULL COMMENT '提交需求的客户 ID',
  `project_type` VARCHAR(100) NOT NULL COMMENT '项目类型',
  `industry` VARCHAR(100) NOT NULL COMMENT '所属行业',
  `requirement_description` TEXT NOT NULL COMMENT '需求描述',
  `budget_range` VARCHAR(100) NOT NULL COMMENT '预算范围',
  `project_cycle` VARCHAR(100) NOT NULL COMMENT '项目周期',
  `status` VARCHAR(32) NOT NULL COMMENT '状态：MATCHED / ACCEPTED',
  `matched_designer_id` BIGINT NOT NULL COMMENT '匹配设计师 ID',
  `human_chat_id` VARCHAR(64) NOT NULL COMMENT '人工咨询会话业务 ID',
  `greeting_messages` JSON NOT NULL COMMENT '交接问候语列表',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultant_intake_human_chat` (`human_chat_id`),
  KEY `idx_consultant_intake_customer` (`customer_id`),
  KEY `idx_consultant_intake_designer_created` (`matched_designer_id`, `created_at`),
  CONSTRAINT `fk_consultant_intake_customer` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_consultant_intake_designer` FOREIGN KEY (`matched_designer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_consultant_intake_status` CHECK (`status` IN ('MATCHED', 'ACCEPTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询需求单表';

CREATE TABLE `consultant_human_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `human_chat_id` VARCHAR(64) NOT NULL COMMENT '人工咨询会话业务 ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送人 ID',
  `sender_role` VARCHAR(32) NOT NULL COMMENT '发送方角色：CUSTOMER / DESIGNER',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_consultant_message_chat_created` (`human_chat_id`, `created_at`, `id`),
  KEY `idx_consultant_message_sender` (`sender_id`),
  CONSTRAINT `fk_consultant_message_chat` FOREIGN KEY (`human_chat_id`) REFERENCES `consultant_intake` (`human_chat_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_consultant_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_consultant_message_role` CHECK (`sender_role` IN ('CUSTOMER', 'DESIGNER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询人工消息表';

CREATE TABLE `file_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uploader_id` BIGINT NOT NULL COMMENT '上传人 ID',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `storage_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
  `storage_provider` VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式：LOCAL / MINIO / OSS',
  `bucket_name` VARCHAR(128) DEFAULT NULL COMMENT '存储桶名称',
  `object_key` VARCHAR(512) NOT NULL COMMENT '对象存储 key 或本地相对路径',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '访问地址',
  `mime_type` VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
  `file_hash` VARCHAR(128) DEFAULT NULL COMMENT '文件哈希，用于去重或完整性校验',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / DELETED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_asset_uploader` (`uploader_id`),
  KEY `idx_file_asset_status` (`status`),
  KEY `idx_file_asset_hash` (`file_hash`),
  CONSTRAINT `fk_file_asset_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_file_asset_provider` CHECK (`storage_provider` IN ('LOCAL', 'MINIO', 'OSS')),
  CONSTRAINT `chk_file_asset_status` CHECK (`status` IN ('ACTIVE', 'DELETED')),
  CONSTRAINT `chk_file_asset_size` CHECK (`file_size` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资产表';

CREATE TABLE `portfolio_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(128) NOT NULL COMMENT '案例标题',
  `category` VARCHAR(32) NOT NULL COMMENT '案例分类：BRAND / DIGITAL / OFFLINE',
  `industry` VARCHAR(64) NOT NULL COMMENT '行业',
  `style` VARCHAR(64) NOT NULL COMMENT '风格',
  `service_type` VARCHAR(64) NOT NULL COMMENT '服务类型',
  `cover_url` VARCHAR(512) NOT NULL COMMENT '封面图地址',
  `image_urls` JSON DEFAULT NULL COMMENT '详情图片地址列表',
  `description` TEXT NOT NULL COMMENT '设计说明',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `featured` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首页精选：0 否，1 是',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT / PUBLISHED / OFFLINE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_portfolio_category` (`category`),
  KEY `idx_portfolio_industry` (`industry`),
  KEY `idx_portfolio_style` (`style`),
  KEY `idx_portfolio_service_type` (`service_type`),
  KEY `idx_portfolio_status_featured_sort` (`status`, `featured`, `sort_order`),
  CONSTRAINT `chk_portfolio_category` CHECK (`category` IN ('BRAND', 'DIGITAL', 'OFFLINE')),
  CONSTRAINT `chk_portfolio_featured` CHECK (`featured` IN (0, 1)),
  CONSTRAINT `chk_portfolio_status` CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
  FULLTEXT KEY `ft_portfolio_title_description` (`title`, `description`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品案例表';

CREATE TABLE `project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(128) NOT NULL COMMENT '项目名称',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID',
  `designer_id` BIGINT NOT NULL COMMENT '设计师 ID',
  `description` TEXT DEFAULT NULL COMMENT '项目说明',
  `current_stage` VARCHAR(64) NOT NULL COMMENT '当前阶段编码',
  `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '项目状态：IN_PROGRESS / COMPLETED / PAUSED / CANCELLED',
  `progress` INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_customer` (`customer_id`),
  KEY `idx_project_designer` (`designer_id`),
  KEY `idx_project_current_stage` (`current_stage`),
  KEY `idx_project_status` (`status`),
  CONSTRAINT `fk_project_customer` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_project_designer` FOREIGN KEY (`designer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_project_progress` CHECK (`progress` >= 0 AND `progress` <= 100),
  CONSTRAINT `chk_project_status` CHECK (`status` IN ('IN_PROGRESS', 'COMPLETED', 'PAUSED', 'CANCELLED')),
  CONSTRAINT `chk_project_current_stage` CHECK (`current_stage` IN (
    'REQUIREMENT_GUIDE',
    'CONTRACT_PREPAYMENT',
    'RESEARCH_REPORT',
    'SKETCH_STYLE',
    'REVIEW_FINAL',
    'FINAL_PAYMENT',
    'AFTER_SALE_REPURCHASE'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目表';

CREATE TABLE `conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户 ID',
  `designer_id` BIGINT NOT NULL COMMENT '设计师 ID',
  `last_message` VARCHAR(255) DEFAULT NULL COMMENT '最近消息摘要',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最近消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_project` (`project_id`),
  KEY `idx_conversation_customer` (`customer_id`),
  KEY `idx_conversation_designer` (`designer_id`),
  KEY `idx_conversation_last_message_at` (`last_message_at`),
  CONSTRAINT `fk_conversation_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversation_customer` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_conversation_designer` FOREIGN KEY (`designer_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表';

CREATE TABLE `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id` BIGINT NOT NULL COMMENT '会话 ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送人 ID，系统消息为空',
  `sender_role` VARCHAR(32) NOT NULL COMMENT '发送方角色：CUSTOMER / DESIGNER / SYSTEM',
  `message_type` VARCHAR(32) NOT NULL COMMENT '消息类型：TEXT / IMAGE / FILE / EMOJI / SYSTEM',
  `content` TEXT DEFAULT NULL COMMENT '消息内容',
  `reply_to_message_id` BIGINT DEFAULT NULL COMMENT '回复的消息 ID，后续扩展预留',
  `client_message_id` VARCHAR(128) DEFAULT NULL COMMENT '客户端消息 ID，用于 WebSocket 或重试去重',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0 否，1 是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_client_message_id` (`client_message_id`),
  KEY `idx_message_conversation_created` (`conversation_id`, `created_at`),
  KEY `idx_message_sender` (`sender_id`),
  KEY `idx_message_reply_to` (`reply_to_message_id`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_message_reply_to` FOREIGN KEY (`reply_to_message_id`) REFERENCES `message` (`id`),
  CONSTRAINT `chk_message_sender_role` CHECK (`sender_role` IN ('CUSTOMER', 'DESIGNER', 'SYSTEM')),
  CONSTRAINT `chk_message_type` CHECK (`message_type` IN ('TEXT', 'IMAGE', 'FILE', 'EMOJI', 'SYSTEM')),
  CONSTRAINT `chk_message_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';

CREATE TABLE `message_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id` BIGINT NOT NULL COMMENT '消息 ID',
  `file_id` BIGINT NOT NULL COMMENT '文件 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_file` (`message_id`, `file_id`),
  KEY `idx_message_file_file` (`file_id`),
  CONSTRAINT `fk_message_file_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_message_file_file` FOREIGN KEY (`file_id`) REFERENCES `file_asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息附件关联表';

CREATE TABLE `conversation_read_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id` BIGINT NOT NULL COMMENT '会话 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `last_read_message_id` BIGINT DEFAULT NULL COMMENT '最后已读消息 ID',
  `last_read_at` DATETIME DEFAULT NULL COMMENT '最后已读时间',
  `unread_count` INT NOT NULL DEFAULT 0 COMMENT '未读数量缓存',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_read_user` (`conversation_id`, `user_id`),
  KEY `idx_conversation_read_message` (`last_read_message_id`),
  KEY `idx_conversation_read_user` (`user_id`),
  CONSTRAINT `fk_conversation_read_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversation_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_conversation_read_message` FOREIGN KEY (`last_read_message_id`) REFERENCES `message` (`id`),
  CONSTRAINT `chk_conversation_read_unread` CHECK (`unread_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话已读状态表';

CREATE TABLE `project_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码',
  `stage_name` VARCHAR(64) NOT NULL COMMENT '阶段名称',
  `sort_order` INT NOT NULL COMMENT '阶段顺序',
  `status` VARCHAR(32) NOT NULL DEFAULT 'TODO' COMMENT '阶段状态：TODO / PENDING_CONFIRM / REACHED / REJECTED',
  `reached_at` DATETIME DEFAULT NULL COMMENT '达成时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_stage_code` (`project_id`, `stage_code`),
  KEY `idx_project_stage_project_sort` (`project_id`, `sort_order`),
  KEY `idx_project_stage_status` (`status`),
  CONSTRAINT `fk_project_stage_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_project_stage_code` CHECK (`stage_code` IN (
    'REQUIREMENT_GUIDE',
    'CONTRACT_PREPAYMENT',
    'RESEARCH_REPORT',
    'SKETCH_STYLE',
    'REVIEW_FINAL',
    'FINAL_PAYMENT',
    'AFTER_SALE_REPURCHASE'
  )),
  CONSTRAINT `chk_project_stage_status` CHECK (`status` IN ('TODO', 'PENDING_CONFIRM', 'REACHED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目阶段表';

CREATE TABLE `stage_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `project_stage_id` BIGINT NOT NULL COMMENT '项目阶段 ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码，冗余便于查询',
  `initiator_id` BIGINT NOT NULL COMMENT '发起人 ID',
  `initiator_role` VARCHAR(32) NOT NULL COMMENT '发起方角色：CUSTOMER / DESIGNER',
  `confirm_user_id` BIGINT NOT NULL COMMENT '确认人 ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '动作状态：PENDING / CONFIRMED / REJECTED / CANCELLED',
  `request_note` VARCHAR(500) DEFAULT NULL COMMENT '发起说明',
  `response_note` VARCHAR(500) DEFAULT NULL COMMENT '确认或驳回说明',
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `responded_at` DATETIME DEFAULT NULL COMMENT '响应时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_stage_action_project_stage` (`project_id`, `stage_code`),
  KEY `idx_stage_action_stage` (`project_stage_id`),
  KEY `idx_stage_action_status` (`status`),
  KEY `idx_stage_action_initiator` (`initiator_id`),
  KEY `idx_stage_action_confirm_user` (`confirm_user_id`),
  CONSTRAINT `fk_stage_action_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_stage_action_stage` FOREIGN KEY (`project_stage_id`) REFERENCES `project_stage` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_stage_action_initiator` FOREIGN KEY (`initiator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_stage_action_confirm_user` FOREIGN KEY (`confirm_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_stage_action_role` CHECK (`initiator_role` IN ('CUSTOMER', 'DESIGNER')),
  CONSTRAINT `chk_stage_action_status` CHECK (`status` IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段确认动作表';

CREATE TABLE `project_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `project_stage_id` BIGINT DEFAULT NULL COMMENT '项目阶段 ID',
  `stage_code` VARCHAR(64) DEFAULT NULL COMMENT '阶段编码，阶段外通用文件可为空',
  `file_id` BIGINT NOT NULL COMMENT '文件 ID',
  `uploader_id` BIGINT NOT NULL COMMENT '上传人 ID',
  `file_role` VARCHAR(32) NOT NULL DEFAULT 'DELIVERABLE' COMMENT '文件用途：MATERIAL / REPORT / DRAFT / FINAL / CONTRACT / DELIVERABLE / OTHER',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '文件说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_file_project_stage` (`project_id`, `stage_code`),
  KEY `idx_project_file_stage` (`project_stage_id`),
  KEY `idx_project_file_file` (`file_id`),
  KEY `idx_project_file_uploader` (`uploader_id`),
  CONSTRAINT `fk_project_file_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_file_stage` FOREIGN KEY (`project_stage_id`) REFERENCES `project_stage` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_project_file_file` FOREIGN KEY (`file_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `fk_project_file_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_project_file_role` CHECK (`file_role` IN ('MATERIAL', 'REPORT', 'DRAFT', 'FINAL', 'CONTRACT', 'DELIVERABLE', 'OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目文件归档表';

CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 ID，系统操作可为空',
  `operator_role` VARCHAR(32) NOT NULL COMMENT '操作方角色：CUSTOMER / DESIGNER / SYSTEM',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型，例如 PROJECT / STAGE / MESSAGE / FILE',
  `biz_id` BIGINT DEFAULT NULL COMMENT '业务 ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作动作',
  `description` VARCHAR(1000) DEFAULT NULL COMMENT '操作描述',
  `before_data` JSON DEFAULT NULL COMMENT '变更前数据',
  `after_data` JSON DEFAULT NULL COMMENT '变更后数据',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_operator` (`operator_id`),
  KEY `idx_operation_log_biz` (`biz_type`, `biz_id`),
  KEY `idx_operation_log_created` (`created_at`),
  CONSTRAINT `fk_operation_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_operation_log_role` CHECK (`operator_role` IN ('CUSTOMER', 'DESIGNER', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

INSERT INTO `user` (`id`, `email`, `phone`, `password_hash`, `nickname`, `role`, `avatar`, `status`, `last_login_at`, `created_at`, `updated_at`) VALUES
(1, 'customer@163.com', NULL, '$2a$10$jFVkPRlTCuJNU3/bc97SZO4GjjiK9QRRIk8pH82/AUt5Efxlxttte', '演示客户', 'CUSTOMER', 'https://example.com/avatar/customer.png', 'ENABLED', NULL, '2026-07-20 10:00:00', '2026-07-20 10:00:00'),
(2, 'designer@example.com', NULL, '$2a$10$ycsJGmPT5IGSN1bN5vTwA.J.8v83fnmr2RtMDGk3OLbPvjrc5en6S', '演示设计师', 'DESIGNER', 'https://example.com/avatar/designer.png', 'ENABLED', NULL, '2026-07-20 10:00:00', '2026-07-20 10:00:00');

INSERT INTO `designer_profile` (`designer_id`, `enabled`, `online`, `specialties`) VALUES
(2, 1, 1, JSON_ARRAY('品牌设计', '海报设计', '餐饮', '教育'));

INSERT INTO `file_asset` (`id`, `uploader_id`, `original_name`, `storage_name`, `storage_provider`, `bucket_name`, `object_key`, `url`, `mime_type`, `file_size`, `file_hash`, `status`, `created_at`, `updated_at`) VALUES
(1, 2, '山野咖啡资料调研报告.pdf', 'project-1-research-report.pdf', 'LOCAL', NULL, 'demo/project-1/project-1-research-report.pdf', 'https://example.com/files/project-1-research-report.pdf', 'application/pdf', 2483200, 'demo-hash-project-1-report', 'ACTIVE', '2026-07-20 11:20:00', '2026-07-20 11:20:00'),
(2, 2, '启星教育草图方向稿.zip', 'project-2-sketch-draft.zip', 'LOCAL', NULL, 'demo/project-2/project-2-sketch-draft.zip', 'https://example.com/files/project-2-sketch-draft.zip', 'application/zip', 5242880, 'demo-hash-project-2-draft', 'ACTIVE', '2026-07-20 11:30:00', '2026-07-20 11:30:00');

INSERT INTO `portfolio_case` (`id`, `title`, `category`, `industry`, `style`, `service_type`, `cover_url`, `image_urls`, `description`, `sort_order`, `featured`, `status`, `created_at`, `updated_at`) VALUES
(1, '山野咖啡品牌视觉升级', 'BRAND', '餐饮', '极简', '品牌设计', 'https://example.com/portfolio/cafe-cover.jpg', JSON_ARRAY('https://example.com/portfolio/cafe-1.jpg', 'https://example.com/portfolio/cafe-2.jpg'), '为精品咖啡品牌重构 Logo、主视觉和门店物料，突出自然、手作和社区感。', 1, 1, 'PUBLISHED', '2026-07-20 10:05:00', '2026-07-20 10:05:00'),
(2, '启星少儿教育招生海报', 'DIGITAL', '教育', '年轻化', '海报设计', 'https://example.com/portfolio/education-cover.jpg', JSON_ARRAY('https://example.com/portfolio/education-1.jpg', 'https://example.com/portfolio/education-2.jpg'), '围绕暑期招生场景设计线上线下海报，强化课程亮点和行动入口。', 2, 0, 'PUBLISHED', '2026-07-20 10:06:00', '2026-07-20 10:06:00'),
(3, '云栖地产高端画册', 'OFFLINE', '地产', '高端', '画册设计', 'https://example.com/portfolio/estate-cover.jpg', JSON_ARRAY('https://example.com/portfolio/estate-1.jpg', 'https://example.com/portfolio/estate-2.jpg'), '为高端住宅项目设计招商画册，强调空间质感、区位价值和生活方式。', 3, 1, 'PUBLISHED', '2026-07-20 10:07:00', '2026-07-20 10:07:00'),
(4, '潮玩电商活动视觉', 'DIGITAL', '电商', '国潮', '活动物料', 'https://example.com/portfolio/ecommerce-cover.jpg', JSON_ARRAY('https://example.com/portfolio/ecommerce-1.jpg', 'https://example.com/portfolio/ecommerce-2.jpg'), '为电商大促设计主 KV、商品卡片和社媒传播图，提升点击和转化。', 4, 1, 'PUBLISHED', '2026-07-20 10:08:00', '2026-07-20 10:08:00'),
(5, '智造科技企业 VI 系统', 'BRAND', '科技', '商务', 'VI 设计', 'https://example.com/portfolio/tech-cover.jpg', JSON_ARRAY('https://example.com/portfolio/tech-1.jpg', 'https://example.com/portfolio/tech-2.jpg'), '为工业科技企业建立统一 VI 系统，覆盖名片、PPT、展板和官网视觉规范。', 5, 0, 'PUBLISHED', '2026-07-20 10:09:00', '2026-07-20 10:09:00'),
(6, '新锐美妆包装设计', 'OFFLINE', '美妆', '年轻化', '包装设计', 'https://example.com/portfolio/beauty-cover.jpg', JSON_ARRAY('https://example.com/portfolio/beauty-1.jpg', 'https://example.com/portfolio/beauty-2.jpg'), '围绕年轻女性消费场景打造包装视觉，突出轻盈、清洁和系列化陈列效果。', 6, 0, 'PUBLISHED', '2026-07-20 10:10:00', '2026-07-20 10:10:00');

INSERT INTO `project` (`id`, `name`, `customer_id`, `designer_id`, `description`, `current_stage`, `status`, `progress`, `created_at`, `updated_at`) VALUES
(1, '山野咖啡品牌升级项目', 1, 2, '精品咖啡品牌视觉升级，覆盖 Logo、门店物料和线上传播图。', 'RESEARCH_REPORT', 'IN_PROGRESS', 28, '2026-07-20 10:20:00', '2026-07-20 11:20:00'),
(2, '启星教育暑期招生海报项目', 1, 2, '暑期招生海报和活动视觉设计，突出课程卖点和报名转化。', 'SKETCH_STYLE', 'IN_PROGRESS', 43, '2026-07-20 10:30:00', '2026-07-20 11:30:00');

INSERT INTO `conversation` (`id`, `project_id`, `customer_id`, `designer_id`, `last_message`, `last_message_at`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 2, '资料调研报告已提交，请客户确认。', '2026-07-20 11:20:00', '2026-07-20 10:20:00', '2026-07-20 11:20:00'),
(2, 2, 1, 2, '草图方向需要再年轻化一点。', '2026-07-20 11:30:00', '2026-07-20 10:30:00', '2026-07-20 11:30:00');

INSERT INTO `message` (`id`, `conversation_id`, `sender_id`, `sender_role`, `message_type`, `content`, `created_at`) VALUES
(1, 1, NULL, 'SYSTEM', 'SYSTEM', '项目已创建，双方可以开始需求沟通。', '2026-07-20 10:20:00'),
(2, 1, 1, 'CUSTOMER', 'TEXT', '我们希望咖啡品牌整体更自然，适合社区门店和线上传播。', '2026-07-20 10:25:00'),
(3, 1, 2, 'DESIGNER', 'TEXT', '收到，我会先整理需求模板，并补充品牌调研方向。', '2026-07-20 10:28:00'),
(4, 1, NULL, 'SYSTEM', 'SYSTEM', '阶段「需求引导」已达成。', '2026-07-20 10:40:00'),
(5, 1, NULL, 'SYSTEM', 'SYSTEM', '阶段「签订合同预付款」已达成。', '2026-07-20 11:00:00'),
(6, 1, 2, 'DESIGNER', 'TEXT', '资料调研报告已提交，请客户确认。', '2026-07-20 11:20:00'),
(7, 2, NULL, 'SYSTEM', 'SYSTEM', '项目已创建，双方可以开始需求沟通。', '2026-07-20 10:30:00'),
(8, 2, 1, 'CUSTOMER', 'TEXT', '这次招生海报需要更活泼，突出暑期课程优惠。', '2026-07-20 10:35:00'),
(9, 2, 2, 'DESIGNER', 'TEXT', '我会提供两个风格方向，一个偏清爽，一个偏高饱和。', '2026-07-20 10:50:00'),
(10, 2, NULL, 'SYSTEM', 'SYSTEM', '阶段「需求引导」已达成。', '2026-07-20 11:00:00'),
(11, 2, NULL, 'SYSTEM', 'SYSTEM', '阶段「资料调研报告」已驳回。', '2026-07-20 11:15:00'),
(12, 2, 1, 'CUSTOMER', 'TEXT', '草图方向需要再年轻化一点。', '2026-07-20 11:30:00');

INSERT INTO `conversation_read_state` (`conversation_id`, `user_id`, `last_read_message_id`, `last_read_at`, `unread_count`, `updated_at`) VALUES
(1, 1, 5, '2026-07-20 11:05:00', 1, '2026-07-20 11:20:00'),
(1, 2, 6, '2026-07-20 11:20:00', 0, '2026-07-20 11:20:00'),
(2, 1, 12, '2026-07-20 11:30:00', 0, '2026-07-20 11:30:00'),
(2, 2, 11, '2026-07-20 11:20:00', 1, '2026-07-20 11:30:00');

INSERT INTO `project_stage` (`project_id`, `stage_code`, `stage_name`, `sort_order`, `status`, `reached_at`, `updated_at`) VALUES
(1, 'REQUIREMENT_GUIDE', '需求引导', 1, 'REACHED', '2026-07-20 10:40:00', '2026-07-20 10:40:00'),
(1, 'CONTRACT_PREPAYMENT', '签订合同预付款', 2, 'REACHED', '2026-07-20 11:00:00', '2026-07-20 11:00:00'),
(1, 'RESEARCH_REPORT', '资料调研报告', 3, 'PENDING_CONFIRM', NULL, '2026-07-20 11:20:00'),
(1, 'SKETCH_STYLE', '草图风格敲定', 4, 'TODO', NULL, '2026-07-20 11:20:00'),
(1, 'REVIEW_FINAL', '审稿定稿', 5, 'TODO', NULL, '2026-07-20 11:20:00'),
(1, 'FINAL_PAYMENT', '交付尾款', 6, 'TODO', NULL, '2026-07-20 11:20:00'),
(1, 'AFTER_SALE_REPURCHASE', '售后复购', 7, 'TODO', NULL, '2026-07-20 11:20:00'),
(2, 'REQUIREMENT_GUIDE', '需求引导', 1, 'REACHED', '2026-07-20 11:00:00', '2026-07-20 11:00:00'),
(2, 'CONTRACT_PREPAYMENT', '签订合同预付款', 2, 'REACHED', '2026-07-20 11:05:00', '2026-07-20 11:05:00'),
(2, 'RESEARCH_REPORT', '资料调研报告', 3, 'REJECTED', NULL, '2026-07-20 11:15:00'),
(2, 'SKETCH_STYLE', '草图风格敲定', 4, 'PENDING_CONFIRM', NULL, '2026-07-20 11:30:00'),
(2, 'REVIEW_FINAL', '审稿定稿', 5, 'TODO', NULL, '2026-07-20 11:30:00'),
(2, 'FINAL_PAYMENT', '交付尾款', 6, 'TODO', NULL, '2026-07-20 11:30:00'),
(2, 'AFTER_SALE_REPURCHASE', '售后复购', 7, 'TODO', NULL, '2026-07-20 11:30:00');

INSERT INTO `stage_action` (`project_id`, `project_stage_id`, `stage_code`, `initiator_id`, `initiator_role`, `confirm_user_id`, `status`, `request_note`, `response_note`, `requested_at`, `responded_at`, `created_at`, `updated_at`) VALUES
(1, 1, 'REQUIREMENT_GUIDE', 2, 'DESIGNER', 1, 'CONFIRMED', '已发送需求引导模板，请确认。', '确认需求方向。', '2026-07-20 10:35:00', '2026-07-20 10:40:00', '2026-07-20 10:35:00', '2026-07-20 10:40:00'),
(1, 2, 'CONTRACT_PREPAYMENT', 2, 'DESIGNER', 1, 'CONFIRMED', '合同和预付款节点已准备。', '确认进入调研阶段。', '2026-07-20 10:55:00', '2026-07-20 11:00:00', '2026-07-20 10:55:00', '2026-07-20 11:00:00'),
(1, 3, 'RESEARCH_REPORT', 2, 'DESIGNER', 1, 'PENDING', '资料调研报告已提交，请客户确认。', NULL, '2026-07-20 11:20:00', NULL, '2026-07-20 11:20:00', '2026-07-20 11:20:00'),
(2, 8, 'REQUIREMENT_GUIDE', 2, 'DESIGNER', 1, 'CONFIRMED', '已发送招生海报需求引导。', '确认。', '2026-07-20 10:55:00', '2026-07-20 11:00:00', '2026-07-20 10:55:00', '2026-07-20 11:00:00'),
(2, 9, 'CONTRACT_PREPAYMENT', 2, 'DESIGNER', 1, 'CONFIRMED', '合同预付款节点确认。', '确认。', '2026-07-20 11:02:00', '2026-07-20 11:05:00', '2026-07-20 11:02:00', '2026-07-20 11:05:00'),
(2, 10, 'RESEARCH_REPORT', 2, 'DESIGNER', 1, 'REJECTED', '资料调研报告已提交。', '需要补充竞品参考。', '2026-07-20 11:10:00', '2026-07-20 11:15:00', '2026-07-20 11:10:00', '2026-07-20 11:15:00'),
(2, 11, 'SKETCH_STYLE', 2, 'DESIGNER', 1, 'PENDING', '草图方向稿已提交，请确认。', NULL, '2026-07-20 11:30:00', NULL, '2026-07-20 11:30:00', '2026-07-20 11:30:00');

INSERT INTO `project_file` (`project_id`, `project_stage_id`, `stage_code`, `file_id`, `uploader_id`, `file_role`, `description`, `created_at`) VALUES
(1, 3, 'RESEARCH_REPORT', 1, 2, 'REPORT', '山野咖啡资料调研报告演示文件。', '2026-07-20 11:20:00'),
(2, 11, 'SKETCH_STYLE', 2, 2, 'DRAFT', '启星教育草图方向稿演示文件。', '2026-07-20 11:30:00');

INSERT INTO `operation_log` (`operator_id`, `operator_role`, `biz_type`, `biz_id`, `action`, `description`, `before_data`, `after_data`, `created_at`) VALUES
(NULL, 'SYSTEM', 'PROJECT', 1, 'CREATE', '系统初始化山野咖啡品牌升级项目。', NULL, JSON_OBJECT('status', 'IN_PROGRESS'), '2026-07-20 10:20:00'),
(NULL, 'SYSTEM', 'PROJECT', 2, 'CREATE', '系统初始化启星教育暑期招生海报项目。', NULL, JSON_OBJECT('status', 'IN_PROGRESS'), '2026-07-20 10:30:00'),
(2, 'DESIGNER', 'STAGE', 3, 'REQUEST_CONFIRM', '设计师发起资料调研报告确认。', JSON_OBJECT('status', 'TODO'), JSON_OBJECT('status', 'PENDING_CONFIRM'), '2026-07-20 11:20:00'),
(1, 'CUSTOMER', 'STAGE', 10, 'REJECT', '客户驳回资料调研报告。', JSON_OBJECT('status', 'PENDING_CONFIRM'), JSON_OBJECT('status', 'REJECTED'), '2026-07-20 11:15:00');
