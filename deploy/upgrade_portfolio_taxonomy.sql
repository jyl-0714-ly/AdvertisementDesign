USE `advertisement_design`;

ALTER TABLE `portfolio_case`
  ADD COLUMN `category` VARCHAR(32) NULL COMMENT '案例分类：BRAND / DIGITAL / OFFLINE' AFTER `title`,
  ADD COLUMN `featured` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首页精选：0 否，1 是' AFTER `sort_order`;

UPDATE `portfolio_case`
SET `category` = CASE
  WHEN `service_type` IN ('品牌设计', 'VI 设计') THEN 'BRAND'
  WHEN `service_type` IN ('画册设计', '包装设计') THEN 'OFFLINE'
  ELSE 'DIGITAL'
END;

UPDATE `portfolio_case`
SET `featured` = CASE WHEN `id` IN (1, 3, 4) THEN 1 ELSE 0 END;

ALTER TABLE `portfolio_case`
  MODIFY COLUMN `category` VARCHAR(32) NOT NULL COMMENT '案例分类：BRAND / DIGITAL / OFFLINE',
  ADD KEY `idx_portfolio_category` (`category`),
  ADD KEY `idx_portfolio_status_featured_sort` (`status`, `featured`, `sort_order`),
  ADD CONSTRAINT `chk_portfolio_category` CHECK (`category` IN ('BRAND', 'DIGITAL', 'OFFLINE')),
  ADD CONSTRAINT `chk_portfolio_featured` CHECK (`featured` IN (0, 1));
