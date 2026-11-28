-- =====================================================================================
-- NodeSecure - T25..T31: companies, plans, subscriptions, invoices, traceability, reports
-- =====================================================================================
-- Target  : the FreeDB database (MySQL 8.0) administered with phpMyAdmin.
-- Order   : import AFTER database/multitenant_rbac.sql (it needs the `organizations` table).
-- Safety  : ADDITIVE ONLY. No table, column or row is dropped, renamed or rebuilt and no
--           existing value is modified. It can be imported again without side effects.
-- Before  : export a full backup from phpMyAdmin (Export > Custom > Structure and data).
-- Tested  : MigrationScriptIntegrationTest applies this file twice on an isolated MySQL 8
--           container and boots the application with ddl-auto=validate on the result.
-- Not used: procedures, triggers, events, DELIMITER, CREATE DATABASE or USE.
-- =====================================================================================

SET NAMES utf8mb4;

-- -------------------------------------------------------------------------------------
-- 1. T26 - CATALOG OF PLANS (US15: Básico, Premium, Corporativo)
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `subscription_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(30) NOT NULL,
  `name` varchar(60) NOT NULL,
  `monthly_price` decimal(10,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `warehouse_limit` int NOT NULL,
  `sensor_node_limit` int NOT NULL,
  `user_limit` int NOT NULL,
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscription_plans_code` (`code`),
  UNIQUE KEY `uk_subscription_plans_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `subscription_plans` (`code`, `name`, `monthly_price`, `currency`, `warehouse_limit`, `sensor_node_limit`, `user_limit`, `active`)
SELECT p.code, p.name, p.monthly_price, 'USD', p.warehouse_limit, p.sensor_node_limit, p.user_limit, b'1'
FROM (SELECT 'BASIC' AS code, 'Básico' AS name, 49.00 AS monthly_price, 1 AS warehouse_limit, 5 AS sensor_node_limit, 3 AS user_limit
      UNION ALL SELECT 'PREMIUM', 'Premium', 129.00, 5, 30, 15
      UNION ALL SELECT 'CORPORATE', 'Corporativo', 349.00, 25, 200, 100) p
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans s WHERE s.code = p.code);

-- -------------------------------------------------------------------------------------
-- 2. T25 - COMPANIES: legal data of each organization, RUC unique in the platform
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `companies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `legal_name` varchar(150) NOT NULL,
  `tax_id` varchar(11) NOT NULL,
  `contact_email` varchar(120) NOT NULL,
  `country` varchar(2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_companies_tax_id` (`tax_id`),
  UNIQUE KEY `uk_companies_company_id` (`company_id`),
  CONSTRAINT `fk_companies_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------------------------------------
-- 3. T27 - SUBSCRIPTIONS: new nullable columns (existing rows keep their values)
-- -------------------------------------------------------------------------------------
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'plan_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `subscriptions` ADD COLUMN `plan_id` bigint NULL, ADD COLUMN `currency` varchar(3) NULL, ADD COLUMN `started_at` datetime(6) NULL, ADD COLUMN `renews_at` datetime(6) NULL, ADD COLUMN `cancelled_at` datetime(6) NULL, ADD COLUMN `updated_at` datetime(6) NULL, ADD KEY `idx_subscriptions_plan_id` (`plan_id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND INDEX_NAME = 'idx_subscriptions_company_status');
SET @sql := IF(@exists = 0, 'ALTER TABLE `subscriptions` ADD KEY `idx_subscriptions_company_status` (`company_id`, `status`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND CONSTRAINT_NAME = 'fk_subscriptions_plan');
SET @sql := IF(@exists = 0, 'ALTER TABLE `subscriptions` ADD CONSTRAINT `fk_subscriptions_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------------------
-- 4. T28 - INVOICES: one per subscription and period start
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `invoices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `subscription_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `concept` varchar(20) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `currency` varchar(3) NOT NULL,
  `status` varchar(20) NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `period_start` datetime(6) NOT NULL,
  `period_end` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoices_subscription_period` (`subscription_id`, `period_start`),
  KEY `idx_invoices_company_issued` (`company_id`, `issued_at`),
  KEY `idx_invoices_plan_id` (`plan_id`),
  CONSTRAINT `fk_invoices_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `fk_invoices_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `subscriptions` (`id`),
  CONSTRAINT `fk_invoices_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------------------------------------
-- 5. T29/T30 - TRACEABILITY LOG (insert-only from the application; no FK to warehouses so
--    a record survives the deletion of its warehouse)
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `traceability_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `zone_id` bigint DEFAULT NULL,
  `record_type` varchar(40) NOT NULL,
  `event_type` varchar(60) NOT NULL,
  `entity_type` varchar(40) NOT NULL,
  `entity_id` varchar(64) DEFAULT NULL,
  `actor` varchar(120) NOT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `description` varchar(500) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_traceability_company_occurred` (`company_id`, `occurred_at`),
  KEY `idx_traceability_company_warehouse_occurred` (`company_id`, `warehouse_id`, `occurred_at`),
  KEY `idx_traceability_company_type_occurred` (`company_id`, `record_type`, `occurred_at`),
  CONSTRAINT `fk_traceability_records_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------------------------------------
-- 6. T31 - INCIDENT REPORTS generated from the log: new nullable columns + no duplicates
--    (manual reports keep NULL in these columns; MySQL allows repeated NULLs in a unique key)
-- -------------------------------------------------------------------------------------
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incident_reports' AND COLUMN_NAME = 'warehouse_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `incident_reports` ADD COLUMN `warehouse_id` bigint NULL, ADD COLUMN `period_start` datetime(6) NULL, ADD COLUMN `period_end` datetime(6) NULL, ADD COLUMN `incident_count` int NULL, ADD COLUMN `discrepancy_count` int NULL, ADD COLUMN `disconnection_count` int NULL, ADD COLUMN `security_alert_count` int NULL, ADD COLUMN `source` varchar(20) NULL, ADD COLUMN `generated_by` varchar(120) NULL, ADD COLUMN `generated_at` datetime(6) NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incident_reports' AND INDEX_NAME = 'uk_incident_reports_period');
SET @sql := IF(@exists = 0, 'ALTER TABLE `incident_reports` ADD UNIQUE KEY `uk_incident_reports_period` (`company_id`, `warehouse_id`, `period_start`, `period_end`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------------------
-- 7. POST-CHECKS (read only)
-- -------------------------------------------------------------------------------------
SELECT 'subscription plans' AS post_check, COUNT(*) AS total FROM subscription_plans;
SELECT 'subscriptions without plan_id (created before the catalog, still valid)' AS post_check, COUNT(*) AS total
FROM subscriptions WHERE plan_id IS NULL;

-- ROLLBACK (manual, only if needed; removes the new structure, never existing data):
--   DROP TABLE invoices; DROP TABLE traceability_records; DROP TABLE companies;
--   ALTER TABLE subscriptions DROP FOREIGN KEY fk_subscriptions_plan;
--   ALTER TABLE subscriptions DROP INDEX idx_subscriptions_company_status, DROP INDEX idx_subscriptions_plan_id,
--     DROP COLUMN plan_id, DROP COLUMN currency, DROP COLUMN started_at, DROP COLUMN renews_at,
--     DROP COLUMN cancelled_at, DROP COLUMN updated_at;
--   DROP TABLE subscription_plans;
--   ALTER TABLE incident_reports DROP INDEX uk_incident_reports_period, DROP COLUMN warehouse_id, ... (columns of step 6)
