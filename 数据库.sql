-- 广告设计项目协作平台数据库初始化脚本（2.0 全新基线）
-- 适用：MySQL 8.0+，仅用于未上线、无生产数据环境的全量初始化。
-- 重复执行策略：本脚本会关闭外键检查并删除全部业务表，再按依赖顺序重建空库。
-- 警告：这是破坏性初始化，不是升级脚本；不得用于保留数据的环境。
-- 本脚本不创建账号、不写入密码或密码哈希，也不插入支付、签署或其他业务成功数据。

CREATE DATABASE IF NOT EXISTS `advertisement_design`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `advertisement_design`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 先删除 2.0 表，再清理未上线旧基线遗留表。顺序不依赖外键，因为此处已关闭外键检查。
DROP TABLE IF EXISTS `outbox_event`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `agent_escalation`;
DROP TABLE IF EXISTS `agent_execution`;
DROP TABLE IF EXISTS `human_takeover_session`;
DROP TABLE IF EXISTS `payment_event`;
DROP TABLE IF EXISTS `payment_order`;
DROP TABLE IF EXISTS `signature_event`;
DROP TABLE IF EXISTS `contract_version_file`;
DROP TABLE IF EXISTS `contract_version`;
DROP TABLE IF EXISTS `contract`;
DROP TABLE IF EXISTS `quote_version`;
DROP TABLE IF EXISTS `quote`;
DROP TABLE IF EXISTS `change_order_version`;
DROP TABLE IF EXISTS `change_order`;
DROP TABLE IF EXISTS `artifact_confirmation`;
DROP TABLE IF EXISTS `artifact_approval`;
DROP TABLE IF EXISTS `artifact_annotation`;
DROP TABLE IF EXISTS `artifact_version_file`;
DROP TABLE IF EXISTS `artifact_version`;
DROP TABLE IF EXISTS `artifact`;
DROP TABLE IF EXISTS `project_stage_event`;
DROP TABLE IF EXISTS `project_stage_instance`;
DROP TABLE IF EXISTS `conversation_read_state`;
DROP TABLE IF EXISTS `message_attachment`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `project_conversation`;
DROP TABLE IF EXISTS `project_assignment`;
DROP TABLE IF EXISTS `customer_project_member`;
DROP TABLE IF EXISTS `portfolio_case_asset`;
DROP TABLE IF EXISTS `portfolio_case`;
DROP TABLE IF EXISTS `file_asset`;
DROP TABLE IF EXISTS `idempotency_record`;
DROP TABLE IF EXISTS `audit_log`;
DROP TABLE IF EXISTS `project`;
DROP TABLE IF EXISTS `organization_member`;
DROP TABLE IF EXISTS `organization`;
DROP TABLE IF EXISTS `user`;

-- 未上线旧基线遗留表，仅清理，不属于 2.0 模型。
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `consultant_human_message`;
DROP TABLE IF EXISTS `consultation_designer_match`;
DROP TABLE IF EXISTS `consultant_intake`;
DROP TABLE IF EXISTS `designer_profile`;
DROP TABLE IF EXISTS `conversation_read_state`;
DROP TABLE IF EXISTS `message_file`;
DROP TABLE IF EXISTS `project_file`;
DROP TABLE IF EXISTS `stage_action`;
DROP TABLE IF EXISTS `project_stage`;
DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `conversation`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- identity：账号、客户组织及组织成员
-- ============================================================================
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账号主键',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '登录邮箱；为空时不参与唯一约束',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '登录手机号；为空时不参与唯一约束',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希；真实值只由安全注册或重置流程写入',
  `display_name` VARCHAR(64) NOT NULL COMMENT '真实用户显示名称',
  `avatar_file_id` BIGINT DEFAULT NULL COMMENT '头像文件 ID；为避免基础表循环依赖，不在本表声明外键',
  `account_type` VARCHAR(32) NOT NULL COMMENT '平台账号类型：CUSTOMER / DESIGNER / ADMIN',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态：ENABLED / DISABLED / LOCKED',
  `last_login_at` DATETIME(3) DEFAULT NULL COMMENT '最近登录时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_type_status` (`account_type`, `status`),
  CONSTRAINT `chk_user_login_identifier` CHECK (`email` IS NOT NULL OR `phone` IS NOT NULL),
  CONSTRAINT `chk_user_account_type` CHECK (`account_type` IN ('CUSTOMER', 'DESIGNER', 'ADMIN')),
  CONSTRAINT `chk_user_status` CHECK (`status` IN ('ENABLED', 'DISABLED', 'LOCKED')),
  CONSTRAINT `chk_user_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户账号；组织和项目角色不放在账号全局角色中';

CREATE TABLE `organization` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户组织主键',
  `name` VARCHAR(128) NOT NULL COMMENT '组织名称',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / SUSPENDED / CLOSED',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_by` BIGINT NOT NULL COMMENT '创建人账号 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_organization_status` (`status`),
  KEY `idx_organization_created_by` (`created_by`),
  CONSTRAINT `fk_organization_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_organization_status` CHECK (`status` IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
  CONSTRAINT `chk_organization_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户组织';

CREATE TABLE `organization_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '组织成员主键',
  `organization_id` BIGINT NOT NULL COMMENT '组织 ID',
  `user_id` BIGINT NOT NULL COMMENT '客户账号 ID',
  `member_role` VARCHAR(32) NOT NULL COMMENT '组织角色：OWNER / ADMIN / MEMBER',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '成员状态：INVITED / ACTIVE / DISABLED / LEFT',
  `joined_at` DATETIME(3) DEFAULT NULL COMMENT '加入时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organization_member` (`organization_id`, `user_id`),
  UNIQUE KEY `uk_organization_member_id_org` (`id`, `organization_id`),
  KEY `idx_organization_member_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_organization_member_org` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_organization_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_organization_member_role` CHECK (`member_role` IN ('OWNER', 'ADMIN', 'MEMBER')),
  CONSTRAINT `chk_organization_member_status` CHECK (`status` IN ('INVITED', 'ACTIVE', 'DISABLED', 'LEFT')),
  CONSTRAINT `chk_organization_member_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户组织成员关系';

-- ============================================================================
-- project：项目、客户项目成员与设计师分配
-- ============================================================================
CREATE TABLE `project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目主键',
  `organization_id` BIGINT NOT NULL COMMENT '所属客户组织 ID',
  `name` VARCHAR(128) NOT NULL DEFAULT '新项目需求沟通' COMMENT '项目名称',
  `name_source` VARCHAR(16) NOT NULL DEFAULT 'AUTO' COMMENT '名称来源：AUTO / MANUAL',
  `description` TEXT DEFAULT NULL COMMENT '项目摘要；不得代替版本化需求',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '项目状态：ACTIVE / PAUSED / COMPLETED / TERMINATED',
  `confirmed_requirement_version_id` BIGINT DEFAULT NULL COMMENT '已确认需求版本 ID；因产物表后建，此处不声明循环外键',
  `started_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '项目开始时间',
  `paused_at` DATETIME(3) DEFAULT NULL COMMENT '暂停时间',
  `completed_at` DATETIME(3) DEFAULT NULL COMMENT '完成时间',
  `terminated_at` DATETIME(3) DEFAULT NULL COMMENT '终止时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_id_org` (`id`, `organization_id`),
  KEY `idx_project_org_status_updated` (`organization_id`, `status`, `updated_at`),
  KEY `idx_project_status_created` (`status`, `created_at`),
  CONSTRAINT `fk_project_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `chk_project_name_source` CHECK (`name_source` IN ('AUTO', 'MANUAL')),
  CONSTRAINT `chk_project_status` CHECK (`status` IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'TERMINATED')),
  CONSTRAINT `chk_project_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目聚合；不内嵌客户或设计师固定参与者';

CREATE TABLE `customer_project_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户项目成员主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `organization_id` BIGINT NOT NULL COMMENT '项目所属组织 ID；用于数据库级租户一致性约束',
  `organization_member_id` BIGINT NOT NULL COMMENT '组织成员关系 ID',
  `project_role` VARCHAR(32) NOT NULL COMMENT '项目角色：PRIMARY_CONTACT / CONFIRMATION_MEMBER / COMMENT_ONLY / VIEW_ONLY',
  `can_confirm_requirement` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可确认需求版本',
  `can_confirm_report` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可确认调研报告',
  `can_confirm_design` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可确认草图或正式设计',
  `can_sign_contract` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可发起或完成合同签署操作',
  `can_manage_payment` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可查看并操作付款',
  `can_receive_delivery` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '可确认接收交付',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / INACTIVE',
  `active_primary_project_id` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `project_role` = 'PRIMARY_CONTACT' AND `status` = 'ACTIVE' THEN `project_id` ELSE NULL END
  ) STORED COMMENT '用于约束每项目仅一个有效主要联系人',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_project_member` (`project_id`, `organization_member_id`),
  UNIQUE KEY `uk_customer_project_member_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_customer_project_active_primary` (`active_primary_project_id`),
  KEY `idx_customer_project_member_org_member` (`organization_member_id`, `status`),
  KEY `idx_customer_project_member_project_status` (`project_id`, `status`),
  CONSTRAINT `fk_customer_project_member_project_org` FOREIGN KEY (`project_id`, `organization_id`) REFERENCES `project` (`id`, `organization_id`),
  CONSTRAINT `fk_customer_project_member_org_member_org` FOREIGN KEY (`organization_member_id`, `organization_id`) REFERENCES `organization_member` (`id`, `organization_id`),
  CONSTRAINT `chk_customer_project_role` CHECK (`project_role` IN ('PRIMARY_CONTACT', 'CONFIRMATION_MEMBER', 'COMMENT_ONLY', 'VIEW_ONLY')),
  CONSTRAINT `chk_customer_project_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT `chk_customer_project_permissions` CHECK (
    `can_confirm_requirement` IN (0, 1) AND `can_confirm_report` IN (0, 1)
    AND `can_confirm_design` IN (0, 1) AND `can_sign_contract` IN (0, 1)
    AND `can_manage_payment` IN (0, 1) AND `can_receive_delivery` IN (0, 1)
  ),
  CONSTRAINT `chk_customer_project_role_permissions` CHECK (
    (`project_role` NOT IN ('COMMENT_ONLY', 'VIEW_ONLY'))
    OR (`can_confirm_requirement` = 0 AND `can_confirm_report` = 0 AND `can_confirm_design` = 0
        AND `can_sign_contract` = 0 AND `can_manage_payment` = 0 AND `can_receive_delivery` = 0)
  ),
  CONSTRAINT `chk_customer_project_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户项目成员、基础角色和关键动作专项授权';

CREATE TABLE `project_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设计师分配主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `designer_user_id` BIGINT NOT NULL COMMENT '设计师账号 ID',
  `assignment_role` VARCHAR(32) NOT NULL COMMENT '分配角色：PRIMARY_DESIGNER / COLLABORATOR / REVIEWER',
  `authorization_scope` JSON NOT NULL COMMENT '授权范围，如会话、敏感文件、商业信息、审核或修改权限',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / ACTIVE / ENDED / REJECTED',
  `initiated_by_actor_type` VARCHAR(32) NOT NULL COMMENT '发起主体类型',
  `initiated_by_actor_id` BIGINT DEFAULT NULL COMMENT '发起主体 ID；系统或 Agent 可为空',
  `accepted_at` DATETIME(3) DEFAULT NULL COMMENT '接受时间',
  `effective_from` DATETIME(3) DEFAULT NULL COMMENT '生效时间',
  `effective_to` DATETIME(3) DEFAULT NULL COMMENT '结束时间',
  `active_primary_project_id` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `assignment_role` = 'PRIMARY_DESIGNER' AND `status` = 'ACTIVE' THEN `project_id` ELSE NULL END
  ) STORED COMMENT '用于约束每项目至多一个有效主负责设计师',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_assignment_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_project_assignment_active_primary` (`active_primary_project_id`),
  KEY `idx_project_assignment_project_status` (`project_id`, `status`),
  KEY `idx_project_assignment_designer_status` (`designer_user_id`, `status`),
  CONSTRAINT `fk_project_assignment_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_project_assignment_designer` FOREIGN KEY (`designer_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_project_assignment_role` CHECK (`assignment_role` IN ('PRIMARY_DESIGNER', 'COLLABORATOR', 'REVIEWER')),
  CONSTRAINT `chk_project_assignment_status` CHECK (`status` IN ('PENDING', 'ACTIVE', 'ENDED', 'REJECTED')),
  CONSTRAINT `chk_project_assignment_actor_type` CHECK (`initiated_by_actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_project_assignment_period` CHECK (`effective_to` IS NULL OR (`effective_from` IS NOT NULL AND `effective_to` >= `effective_from`)),
  CONSTRAINT `chk_project_assignment_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目设计师分配；项目创建时可以没有任何分配记录';

-- ============================================================================
-- common.storage 与 portfolio：底层文件资产及公开案例
-- ============================================================================
CREATE TABLE `file_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件资产主键',
  `uploader_actor_type` VARCHAR(32) NOT NULL COMMENT '上传主体类型',
  `uploader_actor_id` BIGINT DEFAULT NULL COMMENT '上传主体 ID；系统生成文件可为空',
  `organization_id` BIGINT DEFAULT NULL COMMENT '所属客户组织；公开作品或内部文件可为空',
  `project_id` BIGINT DEFAULT NULL COMMENT '所属项目；发送前草稿、公开作品或内部文件可为空',
  `business_scope` VARCHAR(32) NOT NULL COMMENT '业务范围：PUBLIC_PORTFOLIO / PRIVATE_DRAFT / PROJECT_COMMUNICATION / PROJECT_ARTIFACT / COMMERCIAL / INTERNAL_RUNTIME',
  `visibility` VARCHAR(16) NOT NULL COMMENT '可见性：PUBLIC / ORGANIZATION / PROJECT / INTERNAL',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `storage_provider` VARCHAR(32) NOT NULL COMMENT '存储提供方：LOCAL / MINIO / OSS / S3',
  `storage_zone` VARCHAR(16) NOT NULL COMMENT '物理隔离区：PUBLIC / PRIVATE / INTERNAL；实际桶名由部署配置提供',
  `bucket_name` VARCHAR(128) NOT NULL COMMENT '存储桶或逻辑容器名称',
  `object_key` VARCHAR(512) NOT NULL COMMENT '对象键；不保存临时签名 URL',
  `storage_region` VARCHAR(64) DEFAULT NULL COMMENT '存储区域',
  `storage_class` VARCHAR(32) DEFAULT NULL COMMENT '存储类型',
  `mime_type` VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
  `file_extension` VARCHAR(32) DEFAULT NULL COMMENT '规范化扩展名',
  `file_size` BIGINT NOT NULL COMMENT '字节数',
  `hash_algorithm` VARCHAR(16) NOT NULL DEFAULT 'SHA256' COMMENT '哈希算法',
  `file_hash` VARCHAR(128) NOT NULL COMMENT '内容哈希',
  `encryption_key_ref` VARCHAR(255) DEFAULT NULL COMMENT '密钥管理系统引用；不得保存真实密钥',
  `retention_until` DATETIME(3) DEFAULT NULL COMMENT '最早允许物理删除时间',
  `legal_hold` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '争议或审计锁定',
  `status` VARCHAR(32) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态：UPLOADING / ACTIVE / QUARANTINED / DELETED',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_asset_object` (`storage_provider`, `bucket_name`, `object_key`),
  KEY `idx_file_asset_project_scope_status` (`project_id`, `business_scope`, `status`),
  KEY `idx_file_asset_org_visibility` (`organization_id`, `visibility`),
  KEY `idx_file_asset_hash` (`hash_algorithm`, `file_hash`),
  KEY `idx_file_asset_uploader` (`uploader_actor_type`, `uploader_actor_id`),
  CONSTRAINT `fk_file_asset_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `fk_file_asset_project_org` FOREIGN KEY (`project_id`, `organization_id`) REFERENCES `project` (`id`, `organization_id`),
  CONSTRAINT `chk_file_asset_actor_type` CHECK (`uploader_actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_file_asset_scope` CHECK (`business_scope` IN ('PUBLIC_PORTFOLIO', 'PRIVATE_DRAFT', 'PROJECT_COMMUNICATION', 'PROJECT_ARTIFACT', 'COMMERCIAL', 'INTERNAL_RUNTIME')),
  CONSTRAINT `chk_file_asset_visibility` CHECK (`visibility` IN ('PUBLIC', 'ORGANIZATION', 'PROJECT', 'INTERNAL')),
  CONSTRAINT `chk_file_asset_provider` CHECK (`storage_provider` IN ('LOCAL', 'MINIO', 'OSS', 'S3')),
  CONSTRAINT `chk_file_asset_storage_zone` CHECK (`storage_zone` IN ('PUBLIC', 'PRIVATE', 'INTERNAL')),
  CONSTRAINT `chk_file_asset_status` CHECK (`status` IN ('UPLOADING', 'ACTIVE', 'QUARANTINED', 'DELETED')),
  CONSTRAINT `chk_file_asset_numbers` CHECK (`file_size` >= 0 AND `version` >= 0),
  CONSTRAINT `chk_file_asset_flags` CHECK (`legal_hold` IN (0, 1)),
  CONSTRAINT `chk_file_asset_scope_relation` CHECK (
    (`business_scope` IN ('PROJECT_COMMUNICATION', 'PROJECT_ARTIFACT', 'COMMERCIAL') AND `project_id` IS NOT NULL AND `organization_id` IS NOT NULL AND `visibility` IN ('PROJECT', 'INTERNAL') AND `storage_zone` IN ('PRIVATE', 'INTERNAL'))
    OR (`business_scope` = 'PUBLIC_PORTFOLIO' AND `visibility` = 'PUBLIC' AND `storage_zone` = 'PUBLIC' AND `project_id` IS NULL AND `organization_id` IS NULL)
    OR (`business_scope` = 'PRIVATE_DRAFT' AND `project_id` IS NULL AND `storage_zone` IN ('PRIVATE', 'INTERNAL') AND `visibility` IN ('ORGANIZATION', 'INTERNAL')
        AND (`visibility` <> 'ORGANIZATION' OR `organization_id` IS NOT NULL))
    OR (`business_scope` = 'INTERNAL_RUNTIME' AND `visibility` = 'INTERNAL' AND `storage_zone` = 'INTERNAL'
        AND ((`project_id` IS NULL AND `organization_id` IS NULL)
          OR (`project_id` IS NOT NULL AND `organization_id` IS NOT NULL)))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='底层文件资产元数据；业务关系和访问授权由所属模块校验';

ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_avatar_file` FOREIGN KEY (`avatar_file_id`) REFERENCES `file_asset` (`id`) ON DELETE SET NULL;

CREATE TABLE `portfolio_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公开作品案例主键',
  `title` VARCHAR(128) NOT NULL COMMENT '案例标题',
  `category` VARCHAR(32) NOT NULL COMMENT '分类：BRAND / DIGITAL / OFFLINE',
  `industry` VARCHAR(64) NOT NULL COMMENT '行业',
  `style` VARCHAR(64) NOT NULL COMMENT '风格',
  `service_type` VARCHAR(64) NOT NULL COMMENT '服务类型',
  `cover_file_id` BIGINT NOT NULL COMMENT '公开封面文件 ID',
  `description` TEXT NOT NULL COMMENT '案例说明',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `featured` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '首页精选',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT / PUBLISHED / OFFLINE',
  `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_by` BIGINT NOT NULL COMMENT '创建人账号 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_portfolio_category` (`category`),
  KEY `idx_portfolio_status_featured_sort` (`status`, `featured`, `sort_order`),
  KEY `idx_portfolio_cover_file` (`cover_file_id`),
  FULLTEXT KEY `ft_portfolio_title_description` (`title`, `description`),
  CONSTRAINT `fk_portfolio_cover_file` FOREIGN KEY (`cover_file_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `fk_portfolio_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_portfolio_category` CHECK (`category` IN ('BRAND', 'DIGITAL', 'OFFLINE')),
  CONSTRAINT `chk_portfolio_featured` CHECK (`featured` IN (0, 1)),
  CONSTRAINT `chk_portfolio_status` CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
  CONSTRAINT `chk_portfolio_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公开作品案例；分类基线已直接纳入，无需历史升级脚本';

CREATE TABLE `portfolio_case_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `portfolio_case_id` BIGINT NOT NULL COMMENT '作品案例 ID',
  `file_asset_id` BIGINT NOT NULL COMMENT '公开区文件资产 ID',
  `asset_role` VARCHAR(16) NOT NULL COMMENT 'COVER / DETAIL',
  `display_order` INT NOT NULL DEFAULT 0,
  `caption` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portfolio_case_asset_file` (`portfolio_case_id`, `file_asset_id`),
  UNIQUE KEY `uk_portfolio_case_asset_order` (`portfolio_case_id`, `display_order`),
  CONSTRAINT `fk_portfolio_case_asset_case` FOREIGN KEY (`portfolio_case_id`) REFERENCES `portfolio_case` (`id`),
  CONSTRAINT `fk_portfolio_case_asset_file` FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `chk_portfolio_case_asset_role` CHECK (`asset_role` IN ('COVER', 'DETAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公开作品结构化文件清单；仅关联公共存储区文件';

-- ============================================================================
-- communication：项目唯一主会话、不可变消息、附件与已读状态
-- ============================================================================
CREATE TABLE `project_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目主会话主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID；唯一约束保证每项目至多一个主会话',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / CLOSED',
  `last_message_id` BIGINT DEFAULT NULL COMMENT '最近消息 ID；消息表后建，不声明循环外键',
  `last_message_preview` VARCHAR(255) DEFAULT NULL COMMENT '权限安全的最近消息摘要',
  `last_message_at` DATETIME(3) DEFAULT NULL COMMENT '最近消息时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_conversation_project` (`project_id`),
  UNIQUE KEY `uk_project_conversation_id_project` (`id`, `project_id`),
  KEY `idx_project_conversation_last_message` (`last_message_at`),
  CONSTRAINT `fk_project_conversation_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_project_conversation_status` CHECK (`status` IN ('ACTIVE', 'CLOSED')),
  CONSTRAINT `chk_project_conversation_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目唯一主会话；参与权限从项目成员和有效分配计算';

CREATE TABLE `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息主键',
  `conversation_id` BIGINT NOT NULL COMMENT '项目会话 ID',
  `message_type` VARCHAR(32) NOT NULL COMMENT '消息类型：TEXT / IMAGE / FILE / MIXED / SYSTEM / BUSINESS_CARD',
  `content` TEXT DEFAULT NULL COMMENT '消息文本；附件由关联表承载',
  `customer_display_identity` VARCHAR(64) NOT NULL COMMENT '客户可见身份；团队消息固定为项目服务团队',
  `actor_type` VARCHAR(32) NOT NULL COMMENT '真实主体类型',
  `actor_id` BIGINT DEFAULT NULL COMMENT '真实主体 ID；系统事件可为空',
  `send_source` VARCHAR(32) NOT NULL COMMENT '发送来源：CUSTOMER_UI / DESIGNER_UI / ADMIN_UI / AUTOMATION / EXTERNAL_EVENT / SYSTEM',
  `authorization_basis` JSON DEFAULT NULL COMMENT '发送时权限依据快照',
  `reply_to_message_id` BIGINT DEFAULT NULL COMMENT '回复目标消息 ID',
  `correction_message_id` BIGINT DEFAULT NULL COMMENT '被本消息更正的原消息 ID；更正必须新增消息',
  `client_message_id` VARCHAR(128) DEFAULT NULL COMMENT '客户端幂等消息 ID',
  `sent_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发送时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id_conversation` (`id`, `conversation_id`),
  UNIQUE KEY `uk_message_client_id` (`conversation_id`, `client_message_id`),
  UNIQUE KEY `uk_message_correction_target` (`correction_message_id`),
  KEY `idx_message_conversation_sent` (`conversation_id`, `sent_at`, `id`),
  KEY `idx_message_actor` (`actor_type`, `actor_id`, `sent_at`),
  KEY `idx_message_reply` (`reply_to_message_id`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `project_conversation` (`id`),
  CONSTRAINT `fk_message_reply_conversation` FOREIGN KEY (`reply_to_message_id`, `conversation_id`) REFERENCES `message` (`id`, `conversation_id`),
  CONSTRAINT `fk_message_correction_conversation` FOREIGN KEY (`correction_message_id`, `conversation_id`) REFERENCES `message` (`id`, `conversation_id`),
  CONSTRAINT `chk_message_type` CHECK (`message_type` IN ('TEXT', 'IMAGE', 'FILE', 'MIXED', 'SYSTEM', 'BUSINESS_CARD')),
  CONSTRAINT `chk_message_actor_type` CHECK (`actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_message_send_source` CHECK (`send_source` IN ('CUSTOMER_UI', 'DESIGNER_UI', 'ADMIN_UI', 'AUTOMATION', 'EXTERNAL_EVENT', 'SYSTEM')),
  CONSTRAINT `chk_message_payload` CHECK (`content` IS NOT NULL OR `message_type` IN ('IMAGE', 'FILE', 'MIXED', 'BUSINESS_CARD')),
  CONSTRAINT `chk_message_actor_id` CHECK (
    (`actor_type` = 'SYSTEM_EVENT' AND `actor_id` IS NULL)
    OR (`actor_type` <> 'SYSTEM_EVENT' AND `actor_id` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变消息；禁止原地编辑或软删除，错误通过新更正消息处理';

ALTER TABLE `project_conversation`
  ADD CONSTRAINT `fk_project_conversation_last_message`
  FOREIGN KEY (`last_message_id`, `id`) REFERENCES `message` (`id`, `conversation_id`);

CREATE TABLE `message_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息附件关系主键',
  `message_id` BIGINT NOT NULL COMMENT '消息 ID',
  `file_asset_id` BIGINT NOT NULL COMMENT '文件资产 ID',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '同条消息内展示顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_attachment_file` (`message_id`, `file_asset_id`),
  UNIQUE KEY `uk_message_attachment_order` (`message_id`, `display_order`),
  KEY `idx_message_attachment_file` (`file_asset_id`),
  CONSTRAINT `fk_message_attachment_message` FOREIGN KEY (`message_id`) REFERENCES `message` (`id`),
  CONSTRAINT `fk_message_attachment_file` FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `chk_message_attachment_order` CHECK (`display_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息与聊天附件关系；业务层必须校验草稿所有权、状态和项目关系';

CREATE TABLE `conversation_read_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '已读状态主键',
  `conversation_id` BIGINT NOT NULL COMMENT '项目会话 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户账号 ID',
  `last_read_message_id` BIGINT DEFAULT NULL COMMENT '最后已读消息 ID',
  `last_read_at` DATETIME(3) DEFAULT NULL COMMENT '最后已读时间',
  `unread_count` INT NOT NULL DEFAULT 0 COMMENT '未读数缓存',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_read_user` (`conversation_id`, `user_id`),
  KEY `idx_conversation_read_user` (`user_id`, `updated_at`),
  KEY `idx_conversation_read_message` (`last_read_message_id`),
  CONSTRAINT `fk_conversation_read_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `project_conversation` (`id`),
  CONSTRAINT `fk_conversation_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_conversation_read_message_conversation` FOREIGN KEY (`last_read_message_id`, `conversation_id`) REFERENCES `message` (`id`, `conversation_id`),
  CONSTRAINT `chk_conversation_read_numbers` CHECK (`unread_count` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话成员已读游标与未读缓存';

-- ============================================================================
-- workflow：七阶段当前实例与追加式事实事件
-- ============================================================================
CREATE TABLE `project_stage_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段实例主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '固定七阶段编码',
  `stage_name` VARCHAR(64) NOT NULL COMMENT '客户可见阶段名称',
  `sort_order` TINYINT NOT NULL COMMENT '阶段顺序 1-7',
  `status` VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '阶段当前状态',
  `activation_count` INT NOT NULL DEFAULT 0 COMMENT '累计激活次数；重开时递增',
  `activated_at` DATETIME(3) DEFAULT NULL COMMENT '最近激活时间',
  `completed_at` DATETIME(3) DEFAULT NULL COMMENT '最近完成时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本；状态条件更新失败视为并发冲突',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_stage_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_project_stage_id_project_code` (`id`, `project_id`, `stage_code`),
  UNIQUE KEY `uk_project_stage_code` (`project_id`, `stage_code`),
  UNIQUE KEY `uk_project_stage_order` (`project_id`, `sort_order`),
  KEY `idx_project_stage_status` (`project_id`, `status`),
  CONSTRAINT `fk_project_stage_instance_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_project_stage_code` CHECK (`stage_code` IN (
    'REQUIREMENT_GUIDE', 'CONTRACT_PREPAYMENT', 'RESEARCH_REPORT', 'SKETCH_STYLE',
    'REVIEW_FINAL', 'DELIVERY_FINAL_PAYMENT', 'AFTER_SALE_REPURCHASE'
  )),
  CONSTRAINT `chk_project_stage_order` CHECK (
    (`stage_code` = 'REQUIREMENT_GUIDE' AND `sort_order` = 1)
    OR (`stage_code` = 'CONTRACT_PREPAYMENT' AND `sort_order` = 2)
    OR (`stage_code` = 'RESEARCH_REPORT' AND `sort_order` = 3)
    OR (`stage_code` = 'SKETCH_STYLE' AND `sort_order` = 4)
    OR (`stage_code` = 'REVIEW_FINAL' AND `sort_order` = 5)
    OR (`stage_code` = 'DELIVERY_FINAL_PAYMENT' AND `sort_order` = 6)
    OR (`stage_code` = 'AFTER_SALE_REPURCHASE' AND `sort_order` = 7)
  ),
  CONSTRAINT `chk_project_stage_status` CHECK (`status` IN ('NOT_STARTED', 'ACTIVE', 'WAITING_CUSTOMER', 'PROCESSING', 'UNDER_REVIEW', 'CHANGE_PROCESSING', 'COMPLETED', 'SUSPENDED')),
  CONSTRAINT `chk_project_stage_numbers` CHECK (`activation_count` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='七阶段当前状态；只能由 workflow 明确命令迁移';

CREATE TABLE `project_stage_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段事件主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `stage_instance_id` BIGINT NOT NULL COMMENT '阶段实例 ID',
  `stage_code` VARCHAR(64) NOT NULL COMMENT '阶段编码快照',
  `event_type` VARCHAR(32) NOT NULL COMMENT 'ACTIVATED / WAITING / PROCESSING / REVIEW_REQUESTED / COMPLETED / REOPENED / SUSPENDED / RESUMED',
  `from_status` VARCHAR(32) DEFAULT NULL COMMENT '迁移前状态',
  `to_status` VARCHAR(32) NOT NULL COMMENT '迁移后状态',
  `activation_number` INT NOT NULL COMMENT '对应激活轮次',
  `related_object_type` VARCHAR(64) DEFAULT NULL COMMENT '触发对象类型',
  `related_object_id` BIGINT DEFAULT NULL COMMENT '触发对象 ID',
  `related_object_version` INT DEFAULT NULL COMMENT '触发对象版本号',
  `actor_type` VARCHAR(32) NOT NULL COMMENT '真实操作主体类型',
  `actor_id` BIGINT DEFAULT NULL COMMENT '真实主体 ID；系统事件可为空',
  `source` VARCHAR(32) NOT NULL COMMENT '命令来源：CUSTOMER_UI / DESIGNER_UI / ADMIN_UI / AUTOMATION / EXTERNAL_EVENT / SYSTEM',
  `authorization_basis` JSON DEFAULT NULL COMMENT '权限或前置条件依据快照',
  `reason` VARCHAR(1000) DEFAULT NULL COMMENT '原因或说明',
  `request_id` VARCHAR(128) NOT NULL COMMENT '关联请求 ID',
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_stage_event_request` (`request_id`),
  KEY `idx_project_stage_event_stage_time` (`stage_instance_id`, `occurred_at`, `id`),
  KEY `idx_project_stage_event_project_time` (`project_id`, `occurred_at`, `id`),
  CONSTRAINT `fk_project_stage_event_instance_project` FOREIGN KEY (`stage_instance_id`, `project_id`, `stage_code`) REFERENCES `project_stage_instance` (`id`, `project_id`, `stage_code`),
  CONSTRAINT `chk_project_stage_event_code` CHECK (`stage_code` IN ('REQUIREMENT_GUIDE', 'CONTRACT_PREPAYMENT', 'RESEARCH_REPORT', 'SKETCH_STYLE', 'REVIEW_FINAL', 'DELIVERY_FINAL_PAYMENT', 'AFTER_SALE_REPURCHASE')),
  CONSTRAINT `chk_project_stage_event_type` CHECK (`event_type` IN ('ACTIVATED', 'WAITING', 'PROCESSING', 'REVIEW_REQUESTED', 'COMPLETED', 'REOPENED', 'SUSPENDED', 'RESUMED')),
  CONSTRAINT `chk_project_stage_event_to_status` CHECK (`to_status` IN ('NOT_STARTED', 'ACTIVE', 'WAITING_CUSTOMER', 'PROCESSING', 'UNDER_REVIEW', 'CHANGE_PROCESSING', 'COMPLETED', 'SUSPENDED')),
  CONSTRAINT `chk_project_stage_event_actor` CHECK (`actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_project_stage_event_source` CHECK (`source` IN ('CUSTOMER_UI', 'DESIGNER_UI', 'ADMIN_UI', 'AUTOMATION', 'EXTERNAL_EVENT', 'SYSTEM')),
  CONSTRAINT `chk_project_stage_event_activation` CHECK (`activation_number` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式阶段事实；无 updated_at，不允许普通业务接口修改或删除';

-- ============================================================================
-- artifact：版本化需求、报告、草图、正式设计和交付物
-- ============================================================================
CREATE TABLE `artifact` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产物聚合主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `stage_instance_id` BIGINT DEFAULT NULL COMMENT '所属阶段实例 ID',
  `artifact_type` VARCHAR(32) NOT NULL COMMENT 'REQUIREMENT / RESEARCH_REPORT / SKETCH / FORMAL_DESIGN / DELIVERY / PROJECT_PLAN / OTHER',
  `title` VARCHAR(255) NOT NULL COMMENT '产物标题',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / UNDER_REVIEW / PUBLISHED / SUPERSEDED / ARCHIVED',
  `latest_version_number` INT NOT NULL DEFAULT 0 COMMENT '最新版本号缓存',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '聚合乐观锁版本',
  `created_by_actor_type` VARCHAR(32) NOT NULL COMMENT '创建主体类型',
  `created_by_actor_id` BIGINT DEFAULT NULL COMMENT '创建主体 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artifact_id_project` (`id`, `project_id`),
  KEY `idx_artifact_project_type_status` (`project_id`, `artifact_type`, `status`),
  KEY `idx_artifact_stage` (`stage_instance_id`),
  CONSTRAINT `fk_artifact_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_artifact_stage_project` FOREIGN KEY (`stage_instance_id`, `project_id`) REFERENCES `project_stage_instance` (`id`, `project_id`),
  CONSTRAINT `chk_artifact_type` CHECK (`artifact_type` IN ('REQUIREMENT', 'RESEARCH_REPORT', 'SKETCH', 'FORMAL_DESIGN', 'DELIVERY', 'PROJECT_PLAN', 'OTHER')),
  CONSTRAINT `chk_artifact_status` CHECK (`status` IN ('DRAFT', 'UNDER_REVIEW', 'PUBLISHED', 'SUPERSEDED', 'ARCHIVED')),
  CONSTRAINT `chk_artifact_actor` CHECK (`created_by_actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_artifact_numbers` CHECK (`latest_version_number` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本化项目产物聚合';

CREATE TABLE `artifact_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '不可变产物版本主键',
  `artifact_id` BIGINT NOT NULL COMMENT '产物聚合 ID',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID 冗余用于权限过滤',
  `version_number` INT NOT NULL COMMENT '从 1 递增的版本号',
  `parent_version_id` BIGINT DEFAULT NULL COMMENT '父版本 ID',
  `content` JSON DEFAULT NULL COMMENT '结构化版本内容',
  `content_hash` VARCHAR(128) NOT NULL COMMENT '版本内容及文件清单摘要哈希',
  `publication_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PUBLISHED / WITHDRAWN',
  `generated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否由生成式工具产生；生成版本默认未审核',
  `created_by_actor_type` VARCHAR(32) NOT NULL COMMENT '真实创建主体类型',
  `created_by_actor_id` BIGINT DEFAULT NULL COMMENT '真实创建主体 ID',
  `published_by_user_id` BIGINT DEFAULT NULL COMMENT '发布人账号 ID',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '并发控制版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '版本创建时间',
  `published_at` DATETIME(3) DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artifact_version_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_artifact_version_id_artifact` (`id`, `artifact_id`),
  UNIQUE KEY `uk_artifact_version_number` (`artifact_id`, `version_number`),
  UNIQUE KEY `uk_artifact_version_hash` (`artifact_id`, `content_hash`),
  KEY `idx_artifact_version_project_created` (`project_id`, `created_at`, `id`),
  KEY `idx_artifact_version_parent` (`parent_version_id`),
  CONSTRAINT `fk_artifact_version_artifact_project` FOREIGN KEY (`artifact_id`, `project_id`) REFERENCES `artifact` (`id`, `project_id`),
  CONSTRAINT `fk_artifact_version_parent_artifact` FOREIGN KEY (`parent_version_id`, `artifact_id`) REFERENCES `artifact_version` (`id`, `artifact_id`),
  CONSTRAINT `fk_artifact_version_publisher` FOREIGN KEY (`published_by_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_artifact_version_number` CHECK (`version_number` >= 1 AND `version` >= 0),
  CONSTRAINT `chk_artifact_version_generated` CHECK (`generated` IN (0, 1)),
  CONSTRAINT `chk_artifact_version_status` CHECK (`publication_status` IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN')),
  CONSTRAINT `chk_artifact_version_actor` CHECK (`created_by_actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_artifact_version_publish` CHECK (
    (`publication_status` = 'PUBLISHED' AND `published_at` IS NOT NULL AND `published_by_user_id` IS NOT NULL)
    OR (`publication_status` <> 'PUBLISHED')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变产物版本；发布后修订必须新增版本';

ALTER TABLE `project`
  ADD CONSTRAINT `fk_project_confirmed_requirement_version` FOREIGN KEY (`confirmed_requirement_version_id`, `id`) REFERENCES `artifact_version` (`id`, `project_id`);

CREATE TABLE `artifact_version_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本文件关系主键',
  `artifact_version_id` BIGINT NOT NULL COMMENT '产物版本 ID',
  `file_asset_id` BIGINT NOT NULL COMMENT '文件资产 ID',
  `file_role` VARCHAR(32) NOT NULL COMMENT 'PRIMARY / SOURCE / PREVIEW / SUPPLEMENT / DELIVERY',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artifact_version_file` (`artifact_version_id`, `file_asset_id`),
  UNIQUE KEY `uk_artifact_version_file_order` (`artifact_version_id`, `display_order`),
  KEY `idx_artifact_version_file_asset` (`file_asset_id`),
  CONSTRAINT `fk_artifact_version_file_version` FOREIGN KEY (`artifact_version_id`) REFERENCES `artifact_version` (`id`),
  CONSTRAINT `fk_artifact_version_file_asset` FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `chk_artifact_version_file_role` CHECK (`file_role` IN ('PRIMARY', 'SOURCE', 'PREVIEW', 'SUPPLEMENT', 'DELIVERY')),
  CONSTRAINT `chk_artifact_version_file_order` CHECK (`display_order` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产物版本文件清单；历史版本关系不可静默覆盖';

CREATE TABLE `artifact_annotation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT NOT NULL,
  `artifact_version_id` BIGINT NOT NULL,
  `file_asset_id` BIGINT NOT NULL,
  `annotation_type` VARCHAR(16) NOT NULL,
  `geometry` JSON NOT NULL COMMENT '相对坐标及页面信息',
  `content` VARCHAR(2000) DEFAULT NULL,
  `actor_type` VARCHAR(32) NOT NULL,
  `actor_id` BIGINT NOT NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_artifact_annotation_version_file` (`artifact_version_id`, `file_asset_id`, `created_at`),
  CONSTRAINT `fk_artifact_annotation_version_project` FOREIGN KEY (`artifact_version_id`, `project_id`) REFERENCES `artifact_version` (`id`, `project_id`),
  CONSTRAINT `fk_artifact_annotation_file` FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `chk_artifact_annotation_type` CHECK (`annotation_type` IN ('POINT', 'RECTANGLE', 'FREEHAND', 'TEXT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定具体产物版本和文件的结构化标注';

CREATE TABLE `artifact_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `artifact_version_id` BIGINT NOT NULL COMMENT '被审核的不可变版本 ID',
  `decision` VARCHAR(16) NOT NULL COMMENT 'APPROVED / REJECTED',
  `reviewer_user_id` BIGINT NOT NULL COMMENT '真实审核人账号 ID',
  `assignment_id` BIGINT NOT NULL COMMENT '审核时有效的主设计师或审核授权记录 ID',
  `comment` VARCHAR(1000) DEFAULT NULL COMMENT '审核意见',
  `request_id` VARCHAR(128) NOT NULL COMMENT '幂等请求 ID',
  `decided_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '审核时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artifact_approval_request` (`request_id`),
  KEY `idx_artifact_approval_version_time` (`artifact_version_id`, `decided_at`, `id`),
  CONSTRAINT `fk_artifact_approval_version_project` FOREIGN KEY (`artifact_version_id`, `project_id`) REFERENCES `artifact_version` (`id`, `project_id`),
  CONSTRAINT `fk_artifact_approval_reviewer` FOREIGN KEY (`reviewer_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_artifact_approval_assignment_project` FOREIGN KEY (`assignment_id`, `project_id`) REFERENCES `project_assignment` (`id`, `project_id`),
  CONSTRAINT `chk_artifact_approval_decision` CHECK (`decision` IN ('APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式设计审核记录；草图和正式设计对客发布前必须存在有效批准';

CREATE TABLE `artifact_confirmation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户确认记录主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `artifact_id` BIGINT NOT NULL COMMENT '产物聚合 ID 快照',
  `artifact_version_id` BIGINT NOT NULL COMMENT '被确认的不可变版本 ID',
  `artifact_version_number` INT NOT NULL COMMENT '确认时版本号快照',
  `confirmation_type` VARCHAR(32) NOT NULL COMMENT 'REQUIREMENT / REPORT / SKETCH / FORMAL_DESIGN / DELIVERY_RECEIPT',
  `result` VARCHAR(16) NOT NULL COMMENT 'CONFIRMED / REJECTED',
  `actor_type` VARCHAR(32) NOT NULL COMMENT '真实操作主体类型',
  `actor_id` BIGINT NOT NULL COMMENT '真实操作主体 ID',
  `customer_member_id` BIGINT NOT NULL COMMENT '真实客户项目成员 ID',
  `authorization_basis` JSON NOT NULL COMMENT '专项确认权限依据快照',
  `object_version` BIGINT NOT NULL COMMENT '确认命令携带的产物对象版本',
  `comment` VARCHAR(1000) DEFAULT NULL COMMENT '确认或驳回意见',
  `idempotency_key` VARCHAR(128) NOT NULL COMMENT '客户命令幂等键',
  `confirmed_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '操作时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artifact_confirmation_command` (`project_id`, `idempotency_key`),
  KEY `idx_artifact_confirmation_version_time` (`artifact_version_id`, `confirmed_at`, `id`),
  CONSTRAINT `fk_artifact_confirmation_version_project` FOREIGN KEY (`artifact_version_id`, `project_id`) REFERENCES `artifact_version` (`id`, `project_id`),
  CONSTRAINT `fk_artifact_confirmation_member_project` FOREIGN KEY (`customer_member_id`, `project_id`) REFERENCES `customer_project_member` (`id`, `project_id`),
  CONSTRAINT `chk_artifact_confirmation_type` CHECK (`confirmation_type` IN ('REQUIREMENT', 'REPORT', 'SKETCH', 'FORMAL_DESIGN', 'DELIVERY_RECEIPT')),
  CONSTRAINT `chk_artifact_confirmation_result` CHECK (`result` IN ('CONFIRMED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式客户版本确认；普通对话文字不能替代此记录';

-- ============================================================================
-- commercial：变更单、报价、合同、签署与付款事实
-- ============================================================================
CREATE TABLE `change_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '变更单聚合主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_CONFIRMATION / CONFIRMED / REJECTED / CANCELLED',
  `latest_version_number` INT NOT NULL DEFAULT 0 COMMENT '最新版本号缓存',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_change_order_id_project` (`id`, `project_id`),
  KEY `idx_change_order_project_status` (`project_id`, `status`),
  CONSTRAINT `fk_change_order_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_change_order_status` CHECK (`status` IN ('DRAFT', 'PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED', 'CANCELLED')),
  CONSTRAINT `chk_change_order_numbers` CHECK (`latest_version_number` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本化项目变更单聚合';

CREATE TABLE `change_order_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '变更单版本主键',
  `change_order_id` BIGINT NOT NULL COMMENT '变更单 ID',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `version_number` INT NOT NULL COMMENT '版本号',
  `parent_version_id` BIGINT DEFAULT NULL COMMENT '父版本 ID',
  `change_content` JSON NOT NULL COMMENT '范围、周期、价格及影响的结构化内容',
  `content_hash` VARCHAR(128) NOT NULL COMMENT '内容哈希',
  `created_by_actor_type` VARCHAR(32) NOT NULL COMMENT '创建主体类型',
  `created_by_actor_id` BIGINT DEFAULT NULL COMMENT '创建主体 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_change_order_version` (`change_order_id`, `version_number`),
  UNIQUE KEY `uk_change_order_version_id_order` (`id`, `change_order_id`),
  KEY `idx_change_order_version_project` (`project_id`, `created_at`, `id`),
  CONSTRAINT `fk_change_order_version_order_project` FOREIGN KEY (`change_order_id`, `project_id`) REFERENCES `change_order` (`id`, `project_id`),
  CONSTRAINT `fk_change_order_version_parent_order` FOREIGN KEY (`parent_version_id`, `change_order_id`) REFERENCES `change_order_version` (`id`, `change_order_id`),
  CONSTRAINT `chk_change_order_version_number` CHECK (`version_number` >= 1),
  CONSTRAINT `chk_change_order_version_actor` CHECK (`created_by_actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变变更单版本';

CREATE TABLE `quote` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报价聚合主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / ISSUED / ACCEPTED / REJECTED / EXPIRED / SUPERSEDED',
  `latest_version_number` INT NOT NULL DEFAULT 0 COMMENT '最新版本号缓存',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quote_id_project` (`id`, `project_id`),
  KEY `idx_quote_project_status` (`project_id`, `status`),
  CONSTRAINT `fk_quote_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_quote_status` CHECK (`status` IN ('DRAFT', 'ISSUED', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'SUPERSEDED')),
  CONSTRAINT `chk_quote_numbers` CHECK (`latest_version_number` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本化报价聚合';

CREATE TABLE `quote_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报价版本主键',
  `quote_id` BIGINT NOT NULL COMMENT '报价 ID',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `version_number` INT NOT NULL COMMENT '版本号',
  `parent_version_id` BIGINT DEFAULT NULL COMMENT '父版本 ID',
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT 'ISO 4217 币种',
  `total_amount_minor` BIGINT NOT NULL COMMENT '最小货币单位总金额',
  `line_items` JSON NOT NULL COMMENT '报价明细',
  `valid_until` DATETIME(3) DEFAULT NULL COMMENT '有效期',
  `content_hash` VARCHAR(128) NOT NULL COMMENT '内容哈希',
  `created_by_actor_type` VARCHAR(32) NOT NULL COMMENT '创建主体类型',
  `created_by_actor_id` BIGINT DEFAULT NULL COMMENT '创建主体 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_quote_version_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_quote_version_id_quote` (`id`, `quote_id`),
  UNIQUE KEY `uk_quote_version` (`quote_id`, `version_number`),
  KEY `idx_quote_version_project` (`project_id`, `created_at`, `id`),
  CONSTRAINT `fk_quote_version_quote_project` FOREIGN KEY (`quote_id`, `project_id`) REFERENCES `quote` (`id`, `project_id`),
  CONSTRAINT `fk_quote_version_parent_quote` FOREIGN KEY (`parent_version_id`, `quote_id`) REFERENCES `quote_version` (`id`, `quote_id`),
  CONSTRAINT `chk_quote_version_number` CHECK (`version_number` >= 1 AND `total_amount_minor` >= 0),
  CONSTRAINT `chk_quote_version_actor` CHECK (`created_by_actor_type` IN ('DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变报价版本';

CREATE TABLE `contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同聚合主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `quote_version_id` BIGINT DEFAULT NULL COMMENT '关联已接受报价版本 ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / PENDING_SIGNATURE / PARTIALLY_SIGNED / SIGNED / TERMINATED / VOID',
  `latest_version_number` INT NOT NULL DEFAULT 0 COMMENT '最新版本号缓存',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_id_project` (`id`, `project_id`),
  KEY `idx_contract_project_status` (`project_id`, `status`),
  KEY `idx_contract_quote_version` (`quote_version_id`),
  CONSTRAINT `fk_contract_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_contract_quote_version_project` FOREIGN KEY (`quote_version_id`, `project_id`) REFERENCES `quote_version` (`id`, `project_id`),
  CONSTRAINT `chk_contract_status` CHECK (`status` IN ('DRAFT', 'PENDING_SIGNATURE', 'PARTIALLY_SIGNED', 'SIGNED', 'TERMINATED', 'VOID')),
  CONSTRAINT `chk_contract_numbers` CHECK (`latest_version_number` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本化合同聚合';

CREATE TABLE `contract_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同版本主键',
  `contract_id` BIGINT NOT NULL COMMENT '合同 ID',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `version_number` INT NOT NULL COMMENT '版本号',
  `parent_version_id` BIGINT DEFAULT NULL COMMENT '父版本 ID',
  `contract_terms` JSON NOT NULL COMMENT '结构化合同条款摘要',
  `content_hash` VARCHAR(128) NOT NULL COMMENT '合同内容和附件摘要哈希',
  `created_by_user_id` BIGINT NOT NULL COMMENT '创建人账号 ID',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_version_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_contract_version_id_contract` (`id`, `contract_id`),
  UNIQUE KEY `uk_contract_version` (`contract_id`, `version_number`),
  KEY `idx_contract_version_project` (`project_id`, `created_at`, `id`),
  CONSTRAINT `fk_contract_version_contract_project` FOREIGN KEY (`contract_id`, `project_id`) REFERENCES `contract` (`id`, `project_id`),
  CONSTRAINT `fk_contract_version_parent_contract` FOREIGN KEY (`parent_version_id`, `contract_id`) REFERENCES `contract_version` (`id`, `contract_id`),
  CONSTRAINT `fk_contract_version_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_contract_version_number` CHECK (`version_number` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变合同版本';

CREATE TABLE `contract_version_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '合同版本文件关系主键',
  `contract_version_id` BIGINT NOT NULL COMMENT '合同版本 ID',
  `file_asset_id` BIGINT NOT NULL COMMENT '商业文件资产 ID',
  `file_role` VARCHAR(16) NOT NULL COMMENT 'DRAFT / SIGNABLE / SIGNED',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_version_file` (`contract_version_id`, `file_asset_id`),
  KEY `idx_contract_version_file_asset` (`file_asset_id`),
  CONSTRAINT `fk_contract_version_file_version` FOREIGN KEY (`contract_version_id`) REFERENCES `contract_version` (`id`),
  CONSTRAINT `fk_contract_version_file_asset` FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`),
  CONSTRAINT `chk_contract_version_file_role` CHECK (`file_role` IN ('DRAFT', 'SIGNABLE', 'SIGNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同版本商业附件';

CREATE TABLE `signature_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签署外部事件主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `contract_version_id` BIGINT NOT NULL COMMENT '签署对应不可变合同版本 ID',
  `provider` VARCHAR(64) NOT NULL COMMENT '电子签提供方标识',
  `provider_event_id` VARCHAR(128) NOT NULL COMMENT '提供方事件唯一 ID',
  `provider_envelope_id` VARCHAR(128) DEFAULT NULL COMMENT '提供方签署流程 ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT 'CREATED / SENT / SIGNED / REJECTED / EXPIRED / FAILED',
  `signature_valid` TINYINT(1) NOT NULL COMMENT '回调验签结果',
  `payload_hash` VARCHAR(128) NOT NULL COMMENT '原始回调载荷哈希；不保存密钥',
  `occurred_at` DATETIME(3) NOT NULL COMMENT '提供方事件时间',
  `received_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '平台接收时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_signature_provider_event` (`provider`, `provider_event_id`),
  KEY `idx_signature_contract_time` (`contract_version_id`, `occurred_at`, `id`),
  KEY `idx_signature_project_type` (`project_id`, `event_type`),
  CONSTRAINT `fk_signature_contract_version_project` FOREIGN KEY (`contract_version_id`, `project_id`) REFERENCES `contract_version` (`id`, `project_id`),
  CONSTRAINT `chk_signature_event_type` CHECK (`event_type` IN ('CREATED', 'SENT', 'SIGNED', 'REJECTED', 'EXPIRED', 'FAILED')),
  CONSTRAINT `chk_signature_valid` CHECK (`signature_valid` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式电子签外部事实；SIGNED 仅可来自验签成功的外部事件';

CREATE TABLE `payment_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '付款单主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `contract_version_id` BIGINT NOT NULL COMMENT '合同版本 ID',
  `payment_type` VARCHAR(32) NOT NULL COMMENT 'PREPAYMENT / FINAL_BALANCE / REFUND',
  `merchant_order_no` VARCHAR(64) NOT NULL COMMENT '平台商户订单号',
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT 'ISO 4217 币种',
  `amount_minor` BIGINT NOT NULL COMMENT '最小货币单位金额',
  `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED / PROCESSING / SUCCEEDED / FAILED / CLOSED / REFUND_PROCESSING / REFUNDED',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_id_project` (`id`, `project_id`),
  UNIQUE KEY `uk_payment_order_no` (`merchant_order_no`),
  KEY `idx_payment_project_type_status` (`project_id`, `payment_type`, `status`),
  CONSTRAINT `fk_payment_order_contract_project` FOREIGN KEY (`contract_version_id`, `project_id`) REFERENCES `contract_version` (`id`, `project_id`),
  CONSTRAINT `chk_payment_order_type` CHECK (`payment_type` IN ('PREPAYMENT', 'FINAL_BALANCE', 'REFUND')),
  CONSTRAINT `chk_payment_order_status` CHECK (`status` IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CLOSED', 'REFUND_PROCESSING', 'REFUNDED')),
  CONSTRAINT `chk_payment_order_numbers` CHECK (`amount_minor` > 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付款单当前状态；未知、处理中、失败和成功保持独立';

CREATE TABLE `payment_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付外部事件主键',
  `payment_order_id` BIGINT NOT NULL COMMENT '付款单 ID',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `provider` VARCHAR(64) NOT NULL COMMENT '支付提供方标识',
  `provider_event_id` VARCHAR(128) NOT NULL COMMENT '提供方事件唯一 ID',
  `provider_transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '提供方交易号',
  `event_type` VARCHAR(32) NOT NULL COMMENT 'PROCESSING / SUCCEEDED / FAILED / REFUND_PROCESSING / REFUNDED',
  `signature_valid` TINYINT(1) NOT NULL COMMENT '回调验签结果',
  `payload_hash` VARCHAR(128) NOT NULL COMMENT '原始回调载荷哈希',
  `amount_minor` BIGINT NOT NULL COMMENT '事件金额',
  `currency` CHAR(3) NOT NULL COMMENT '事件币种',
  `occurred_at` DATETIME(3) NOT NULL COMMENT '提供方事件时间',
  `received_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '平台接收时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_provider_event` (`provider`, `provider_event_id`),
  KEY `idx_payment_event_order_time` (`payment_order_id`, `occurred_at`, `id`),
  KEY `idx_payment_event_project_type` (`project_id`, `event_type`),
  CONSTRAINT `fk_payment_event_order_project` FOREIGN KEY (`payment_order_id`, `project_id`) REFERENCES `payment_order` (`id`, `project_id`),
  CONSTRAINT `chk_payment_event_type` CHECK (`event_type` IN ('PROCESSING', 'SUCCEEDED', 'FAILED', 'REFUND_PROCESSING', 'REFUNDED')),
  CONSTRAINT `chk_payment_event_valid` CHECK (`signature_valid` IN (0, 1)),
  CONSTRAINT `chk_payment_event_amount` CHECK (`amount_minor` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式支付外部事实；成功只能由验签且幂等的外部事件产生';

-- ============================================================================
-- automation：人工接管、Agent 执行和升级事项（仅内部可见）
-- ============================================================================
CREATE TABLE `human_takeover_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '人工接管会话主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `conversation_id` BIGINT NOT NULL COMMENT '项目会话 ID',
  `operator_user_id` BIGINT NOT NULL COMMENT '接管操作人账号 ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ENDED',
  `reason` VARCHAR(1000) NOT NULL COMMENT '内部接管原因',
  `active_conversation_id` BIGINT GENERATED ALWAYS AS (
    CASE WHEN `status` = 'ACTIVE' THEN `conversation_id` ELSE NULL END
  ) STORED COMMENT '用于约束每个会话至多一个有效接管',
  `started_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接管开始时间',
  `ended_at` DATETIME(3) DEFAULT NULL COMMENT '接管结束时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_takeover_active_conversation` (`active_conversation_id`),
  KEY `idx_takeover_project_time` (`project_id`, `started_at`, `id`),
  CONSTRAINT `fk_takeover_conversation_project` FOREIGN KEY (`conversation_id`, `project_id`) REFERENCES `project_conversation` (`id`, `project_id`),
  CONSTRAINT `fk_takeover_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_takeover_status` CHECK (`status` IN ('ACTIVE', 'ENDED')),
  CONSTRAINT `chk_takeover_end` CHECK ((`status` = 'ACTIVE' AND `ended_at` IS NULL) OR (`status` = 'ENDED' AND `ended_at` IS NOT NULL)),
  CONSTRAINT `chk_takeover_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内部人工接管；有效期间自动回复必须暂停，客户侧不得暴露';

CREATE TABLE `agent_execution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Agent 执行主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `conversation_id` BIGINT DEFAULT NULL COMMENT '关联会话 ID',
  `stage_instance_id` BIGINT DEFAULT NULL COMMENT '关联阶段实例 ID',
  `agent_type` VARCHAR(32) NOT NULL COMMENT 'COORDINATOR_AGENT / STAGE_AGENT',
  `stage_code` VARCHAR(64) DEFAULT NULL COMMENT '阶段 Agent 对应阶段；协调 Agent 可为空',
  `trigger_type` VARCHAR(32) NOT NULL COMMENT 'MESSAGE / DOMAIN_EVENT / SCHEDULE / MANUAL_RETRY',
  `trigger_id` VARCHAR(128) NOT NULL COMMENT '触发标识',
  `status` VARCHAR(32) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLED',
  `confidence` DECIMAL(5,4) DEFAULT NULL COMMENT '内部置信度，仅高级运行诊断可见',
  `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '安全的内部失败码',
  `diagnostic_ref` VARCHAR(255) DEFAULT NULL COMMENT '受控诊断存储引用；不直接存完整提示或私有正文',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `started_at` DATETIME(3) DEFAULT NULL COMMENT '开始时间',
  `finished_at` DATETIME(3) DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_execution_trigger` (`agent_type`, `trigger_type`, `trigger_id`),
  UNIQUE KEY `uk_agent_execution_id_project` (`id`, `project_id`),
  KEY `idx_agent_execution_project_status` (`project_id`, `status`, `created_at`),
  CONSTRAINT `fk_agent_execution_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_agent_execution_conversation_project` FOREIGN KEY (`conversation_id`, `project_id`) REFERENCES `project_conversation` (`id`, `project_id`),
  CONSTRAINT `fk_agent_execution_stage_project` FOREIGN KEY (`stage_instance_id`, `project_id`, `stage_code`) REFERENCES `project_stage_instance` (`id`, `project_id`, `stage_code`),
  CONSTRAINT `chk_agent_execution_type` CHECK (`agent_type` IN ('COORDINATOR_AGENT', 'STAGE_AGENT')),
  CONSTRAINT `chk_agent_execution_stage` CHECK (
    (`agent_type` = 'COORDINATOR_AGENT' AND `stage_code` IS NULL AND `stage_instance_id` IS NULL)
    OR (`agent_type` = 'STAGE_AGENT' AND `stage_code` IN ('REQUIREMENT_GUIDE', 'CONTRACT_PREPAYMENT', 'RESEARCH_REPORT', 'SKETCH_STYLE', 'REVIEW_FINAL', 'DELIVERY_FINAL_PAYMENT', 'AFTER_SALE_REPURCHASE') AND `stage_instance_id` IS NOT NULL)
  ),
  CONSTRAINT `chk_agent_execution_trigger` CHECK (`trigger_type` IN ('MESSAGE', 'DOMAIN_EVENT', 'SCHEDULE', 'MANUAL_RETRY')),
  CONSTRAINT `chk_agent_execution_status` CHECK (`status` IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
  CONSTRAINT `chk_agent_execution_confidence` CHECK (`confidence` IS NULL OR (`confidence` >= 0 AND `confidence` <= 1)),
  CONSTRAINT `chk_agent_execution_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内部自动化执行记录；不得进入客户响应或普通设计师详情';

CREATE TABLE `agent_escalation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '升级事项主键',
  `project_id` BIGINT NOT NULL COMMENT '项目 ID',
  `agent_execution_id` BIGINT DEFAULT NULL COMMENT '来源执行 ID',
  `escalation_type` VARCHAR(32) NOT NULL COMMENT 'LOW_CONFIDENCE / REQUIREMENT_CONFLICT / CUSTOMER_DISSATISFACTION / COMMERCIAL_FAILURE / OVERDUE / STAGE_TIMEOUT / SLA_TIMEOUT / OTHER',
  `priority` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW / NORMAL / HIGH / URGENT',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / ACKNOWLEDGED / RESOLVED / CLOSED',
  `summary` VARCHAR(1000) NOT NULL COMMENT '内部升级摘要',
  `assignee_user_id` BIGINT DEFAULT NULL COMMENT '处理人账号 ID',
  `due_at` DATETIME(3) DEFAULT NULL COMMENT 'SLA 截止时间',
  `resolved_at` DATETIME(3) DEFAULT NULL COMMENT '解决时间',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_escalation_project_status_priority` (`project_id`, `status`, `priority`),
  KEY `idx_escalation_assignee_due` (`assignee_user_id`, `status`, `due_at`),
  CONSTRAINT `fk_escalation_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `fk_escalation_execution_project` FOREIGN KEY (`agent_execution_id`, `project_id`) REFERENCES `agent_execution` (`id`, `project_id`),
  CONSTRAINT `fk_escalation_assignee` FOREIGN KEY (`assignee_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `chk_escalation_type` CHECK (`escalation_type` IN ('LOW_CONFIDENCE', 'REQUIREMENT_CONFLICT', 'CUSTOMER_DISSATISFACTION', 'COMMERCIAL_FAILURE', 'OVERDUE', 'STAGE_TIMEOUT', 'SLA_TIMEOUT', 'OTHER')),
  CONSTRAINT `chk_escalation_priority` CHECK (`priority` IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
  CONSTRAINT `chk_escalation_status` CHECK (`status` IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'CLOSED')),
  CONSTRAINT `chk_escalation_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内部升级事项与 SLA 状态';

-- ============================================================================
-- notification：业务通知和事务提交后的可靠事件投递
-- ============================================================================
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知主键',
  `recipient_user_id` BIGINT NOT NULL COMMENT '接收用户 ID',
  `project_id` BIGINT DEFAULT NULL COMMENT '关联项目 ID',
  `notification_type` VARCHAR(64) NOT NULL COMMENT '业务通知类型',
  `channel` VARCHAR(16) NOT NULL COMMENT 'IN_APP / EMAIL / SMS',
  `title` VARCHAR(255) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(2000) NOT NULL COMMENT '不含密钥或内部诊断的通知内容',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SENDING / SENT / FAILED / CANCELLED',
  `deduplication_key` VARCHAR(128) NOT NULL COMMENT '业务去重键',
  `scheduled_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '计划发送时间',
  `sent_at` DATETIME(3) DEFAULT NULL COMMENT '发送时间',
  `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '安全失败码',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_dedup` (`recipient_user_id`, `channel`, `deduplication_key`),
  KEY `idx_notification_dispatch` (`status`, `scheduled_at`, `id`),
  KEY `idx_notification_project_user` (`project_id`, `recipient_user_id`, `created_at`),
  CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_notification_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_notification_channel` CHECK (`channel` IN ('IN_APP', 'EMAIL', 'SMS')),
  CONSTRAINT `chk_notification_status` CHECK (`status` IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'CANCELLED')),
  CONSTRAINT `chk_notification_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户业务通知；外部发送由事务提交后的投递器执行';

CREATE TABLE `outbox_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Outbox 主键',
  `aggregate_type` VARCHAR(64) NOT NULL COMMENT '聚合类型',
  `aggregate_id` BIGINT NOT NULL COMMENT '聚合 ID',
  `event_type` VARCHAR(128) NOT NULL COMMENT '领域事件类型',
  `event_key` VARCHAR(128) NOT NULL COMMENT '事件业务唯一键',
  `payload` JSON NOT NULL COMMENT '最小必要事件载荷；不得含凭据或完整私有正文',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / PUBLISHING / PUBLISHED / FAILED / DEAD',
  `available_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最早投递时间',
  `published_at` DATETIME(3) DEFAULT NULL COMMENT '成功投递时间',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `last_error_code` VARCHAR(64) DEFAULT NULL COMMENT '安全失败码',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '多实例抢占乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbox_event_key` (`event_key`),
  KEY `idx_outbox_dispatch` (`status`, `available_at`, `id`),
  CONSTRAINT `chk_outbox_status` CHECK (`status` IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED', 'DEAD')),
  CONSTRAINT `chk_outbox_numbers` CHECK (`retry_count` >= 0 AND `version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事务内写入、提交后投递的可靠领域事件';

-- ============================================================================
-- common：跨用例幂等结果和追加式审计
-- ============================================================================
CREATE TABLE `idempotency_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '幂等记录主键',
  `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型；FIRST_REQUIREMENT_CREATE 表示首条有效需求原子建项',
  `actor_type` VARCHAR(32) NOT NULL COMMENT '请求主体类型',
  `actor_id` BIGINT NOT NULL COMMENT '请求主体 ID',
  `idempotency_key` VARCHAR(128) NOT NULL COMMENT '调用方幂等键',
  `request_hash` VARCHAR(128) NOT NULL COMMENT '规范化请求摘要；同键不同请求必须冲突',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING / SUCCEEDED / FAILED',
  `resource_type` VARCHAR(64) DEFAULT NULL COMMENT '成功创建的主资源类型',
  `resource_id` BIGINT DEFAULT NULL COMMENT '成功创建的主资源 ID',
  `response_snapshot` JSON DEFAULT NULL COMMENT '安全且最小的幂等响应快照',
  `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '安全业务失败码',
  `expires_at` DATETIME(3) DEFAULT NULL COMMENT '非关键命令可设置过期时间；首需求建项原则上长期保留',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '并发处理乐观锁版本',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次请求时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_actor_operation_key` (`actor_type`, `actor_id`, `operation_type`, `idempotency_key`),
  KEY `idx_idempotency_resource` (`resource_type`, `resource_id`),
  KEY `idx_idempotency_status_updated` (`status`, `updated_at`),
  CONSTRAINT `chk_idempotency_actor` CHECK (`actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_idempotency_status` CHECK (`status` IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT `chk_idempotency_first_requirement_retention` CHECK (`operation_type` <> 'FIRST_REQUIREMENT_CREATE' OR `expires_at` IS NULL),
  CONSTRAINT `chk_idempotency_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关键客户端命令幂等记录；唯一约束防止首条有效需求重复建项';

CREATE TABLE `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计记录主键',
  `project_id` BIGINT DEFAULT NULL COMMENT '关联项目 ID',
  `actor_type` VARCHAR(32) NOT NULL COMMENT '真实主体类型',
  `actor_id` BIGINT DEFAULT NULL COMMENT '真实主体 ID；系统事件可为空',
  `customer_display_identity` VARCHAR(64) DEFAULT NULL COMMENT '动作涉及对客消息时的显示身份快照',
  `source` VARCHAR(32) NOT NULL COMMENT 'CUSTOMER_UI / DESIGNER_UI / ADMIN_UI / AUTOMATION / EXTERNAL_EVENT / SYSTEM',
  `object_type` VARCHAR(64) NOT NULL COMMENT '对象类型',
  `object_id` BIGINT DEFAULT NULL COMMENT '对象 ID',
  `dedup_object_id` BIGINT GENERATED ALWAYS AS (COALESCE(`object_id`, 0)) STORED COMMENT '审计去重用对象 ID；空对象统一为 0',
  `object_version` VARCHAR(64) DEFAULT NULL COMMENT '对象版本或业务版本标识',
  `action` VARCHAR(64) NOT NULL COMMENT '动作',
  `authorization_basis` JSON DEFAULT NULL COMMENT '权限依据快照',
  `before_state` JSON DEFAULT NULL COMMENT '变更前最小必要状态',
  `after_state` JSON DEFAULT NULL COMMENT '变更后最小必要状态',
  `result` VARCHAR(16) NOT NULL COMMENT 'SUCCESS / REJECTED / FAILED',
  `failure_code` VARCHAR(64) DEFAULT NULL COMMENT '安全失败码',
  `request_id` VARCHAR(128) NOT NULL COMMENT '请求 ID',
  `correlation_id` VARCHAR(128) DEFAULT NULL COMMENT '跨模块关联 ID',
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_request_object_action` (`request_id`, `object_type`, `dedup_object_id`, `action`),
  KEY `idx_audit_project_time` (`project_id`, `occurred_at`, `id`),
  KEY `idx_audit_actor_time` (`actor_type`, `actor_id`, `occurred_at`),
  KEY `idx_audit_object_time` (`object_type`, `object_id`, `occurred_at`),
  CONSTRAINT `fk_audit_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`),
  CONSTRAINT `chk_audit_actor` CHECK (`actor_type` IN ('CUSTOMER_USER', 'DESIGNER_USER', 'ADMIN_USER', 'COORDINATOR_AGENT', 'STAGE_AGENT', 'SYSTEM_EVENT')),
  CONSTRAINT `chk_audit_source` CHECK (`source` IN ('CUSTOMER_UI', 'DESIGNER_UI', 'ADMIN_UI', 'AUTOMATION', 'EXTERNAL_EVENT', 'SYSTEM')),
  CONSTRAINT `chk_audit_result` CHECK (`result` IN ('SUCCESS', 'REJECTED', 'FAILED')),
  CONSTRAINT `chk_audit_actor_id` CHECK ((`actor_type` = 'SYSTEM_EVENT' AND `actor_id` IS NULL) OR (`actor_type` <> 'SYSTEM_EVENT' AND `actor_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='追加式独立审计记录；无 updated_at，普通业务接口不得修改或删除';

-- 初始化完成后数据库保持空业务数据。
-- 第一条有效需求必须由应用服务在单一事务中写入项目、成员、主会话、七阶段实例、
-- 首条客户消息、附件关系、首阶段事件、幂等结果、审计记录和必要 Outbox 事件。
-- “每项目恰有一个主会话、恰有七个阶段”属于上述原子建项事务的不变量；唯一键负责防重，
-- 不使用触发器补写业务数据，以免绕过 workflow、审计、幂等及 Outbox 边界。
-- message、project_stage_event、artifact/version 事实、商业外部事件和 audit_log 为追加式记录；
-- 生产数据库账号应按最小权限禁止应用角色 UPDATE/DELETE 这些历史表，仅允许所属应用服务 INSERT/SELECT。
-- file_asset.storage_zone 表示公开、私有、内部物理隔离区；具体 Bucket 名必须由部署配置提供，脚本不写真实配置。
-- FIRST_REQUIREMENT_CREATE 记录由 CHECK 约束禁止设置过期时间；生产清理任务不得删除该操作类型记录。
