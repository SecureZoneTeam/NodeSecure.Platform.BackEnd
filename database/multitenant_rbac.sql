-- =====================================================================================
-- NodeSecure - Multi-tenant + RBAC migration (MySQL 8.0)
-- =====================================================================================
-- Target  : the FreeDB database administered with phpMyAdmin (dump freedb_vS3Zy07C.sql,
--           MySQL 8.0.46). Import it manually from phpMyAdmin with the database selected.
-- Safety  : ADDITIVE ONLY. No table, column or row is dropped, renamed or rebuilt.
--           Existing values are never overwritten: only the NEW columns are filled, and only
--           while they are still empty. The script can be imported again without side effects.
-- Before  : export a full backup from phpMyAdmin (Export > Custom > Structure and data).
-- Tested  : src/test/java/.../MigrationScriptIntegrationTest applies this file twice on an
--           isolated MySQL 8 container loaded with the FreeDB structure and checks the result.
-- Not used: procedures, triggers, events, DELIMITER, CREATE DATABASE or USE.
-- =====================================================================================

SET NAMES utf8mb4;

-- -------------------------------------------------------------------------------------
-- 0. PRE-CHECKS (read only). Review the results before continuing.
-- -------------------------------------------------------------------------------------
SELECT 'users without company_id (they will not be able to sign in)' AS pre_check, COUNT(*) AS total
FROM users WHERE company_id IS NULL;

SELECT 'users whose warehouse_id does not exist in their company (assignment not migrated)' AS pre_check,
       COUNT(*) AS total
FROM users u
LEFT JOIN warehouses w ON w.id = u.warehouse_id AND w.company_id = u.company_id
WHERE u.warehouse_id IS NOT NULL AND w.id IS NULL;

SELECT 'stock_movements whose warehouse does not exist' AS pre_check, COUNT(*) AS total
FROM stock_movements sm LEFT JOIN warehouses w ON w.id = sm.warehouse_id WHERE w.id IS NULL;

SELECT 'companies without a ROLE_ADMINISTRATOR user (no owner will be assigned)' AS pre_check,
       COUNT(*) AS total
FROM (SELECT DISTINCT company_id FROM users WHERE company_id IS NOT NULL) c
WHERE NOT EXISTS (SELECT 1 FROM users a
                  WHERE a.company_id = c.company_id AND FIND_IN_SET('ROLE_ADMINISTRATOR', a.roles) > 0);

-- -------------------------------------------------------------------------------------
-- 1. NEW TABLES
-- -------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `organizations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_organizations_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Warehouses assigned to each worker (scope). The owner sees every warehouse and has no rows here.
CREATE TABLE IF NOT EXISTS `user_warehouses` (
  `user_id` bigint NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `warehouse_id`),
  KEY `idx_user_warehouses_warehouse_id` (`warehouse_id`),
  KEY `idx_user_warehouses_company_id` (`company_id`),
  CONSTRAINT `fk_user_warehouses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_warehouses_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exceptions to the role defaults: granted = 1 adds the permission, granted = 0 removes it.
CREATE TABLE IF NOT EXISTS `user_permissions` (
  `user_id` bigint NOT NULL,
  `permission_code` varchar(60) NOT NULL,
  `granted` bit(1) NOT NULL,
  `company_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `permission_code`),
  KEY `idx_user_permissions_company_id` (`company_id`),
  CONSTRAINT `fk_user_permissions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_permissions_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -------------------------------------------------------------------------------------
-- 2. NEW COLUMNS (each one is added only if it does not exist yet)
-- -------------------------------------------------------------------------------------
-- users.role : OWNER / SUPERVISOR / OPERATOR. The legacy column `roles` is kept untouched.
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role');
SET @sql := IF(@exists = 0, 'ALTER TABLE `users` ADD COLUMN `role` varchar(30) NULL AFTER `roles`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- users.status : ACTIVE / SUSPENDED (existing accounts become ACTIVE).
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'status');
SET @sql := IF(@exists = 0, 'ALTER TABLE `users` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT ''ACTIVE''', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- users.token_version : incremented on suspension so previous tokens stop working.
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'token_version');
SET @sql := IF(@exists = 0, 'ALTER TABLE `users` ADD COLUMN `token_version` bigint NOT NULL DEFAULT 0', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- stock_movements.company_id (+ index)
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements' AND COLUMN_NAME = 'company_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `stock_movements` ADD COLUMN `company_id` bigint NULL AFTER `id`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_movements'
                  AND INDEX_NAME = 'idx_stock_movement_company_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `stock_movements` ADD KEY `idx_stock_movement_company_id` (`company_id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- subscriptions.company_id
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'company_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `subscriptions` ADD COLUMN `company_id` bigint NULL AFTER `id`, ADD KEY `idx_subscriptions_company_id` (`company_id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- profiles.company_id
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profiles' AND COLUMN_NAME = 'company_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `profiles` ADD COLUMN `company_id` bigint NULL AFTER `id`, ADD KEY `idx_profiles_company_id` (`company_id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- incident_reports.company_id
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'incident_reports' AND COLUMN_NAME = 'company_id');
SET @sql := IF(@exists = 0, 'ALTER TABLE `incident_reports` ADD COLUMN `company_id` bigint NULL AFTER `id`, ADD KEY `idx_incident_reports_company_id` (`company_id`)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------------------
-- 3. ORGANIZATIONS: one per existing company_id, keeping the same id.
-- -------------------------------------------------------------------------------------
INSERT INTO `organizations` (`id`, `name`, `owner_user_id`, `status`, `created_at`, `updated_at`)
SELECT c.company_id, CONCAT('Organización ', c.company_id), NULL, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (SELECT company_id FROM users WHERE company_id IS NOT NULL
      UNION SELECT company_id FROM warehouses
      UNION SELECT company_id FROM products
      UNION SELECT company_id FROM sensor_nodes
      UNION SELECT company_id FROM physical_events) c
WHERE NOT EXISTS (SELECT 1 FROM organizations o WHERE o.id = c.company_id);

-- OPTIONAL: give organization 1 its real name (the owner can also rename it later from the app).
-- UPDATE `organizations` SET `name` = 'Nombre real de la empresa' WHERE `id` = 1 AND `name` = 'Organización 1';

-- -------------------------------------------------------------------------------------
-- 4. ROLES (only users whose new `role` column is still empty; `roles` is never modified)
--    - first ROLE_ADMINISTRATOR of each company (lowest id) -> ROLE_OWNER
--    - other ROLE_ADMINISTRATOR and ROLE_SECURITY_MANAGER   -> ROLE_SUPERVISOR
--    - ROLE_WAREHOUSE_KEEPER, ROLE_AUDITOR and any other    -> ROLE_OPERATOR
-- -------------------------------------------------------------------------------------
UPDATE `users` u
JOIN (SELECT a.company_id, MIN(a.id) AS owner_id
      FROM users a
      WHERE a.company_id IS NOT NULL
        AND FIND_IN_SET('ROLE_ADMINISTRATOR', REPLACE(a.roles, ' ', '')) > 0
        AND a.company_id NOT IN (SELECT b.company_id FROM users b
                                 WHERE b.role = 'ROLE_OWNER' AND b.company_id IS NOT NULL)
      GROUP BY a.company_id) first_admin ON first_admin.owner_id = u.id
SET u.role = 'ROLE_OWNER'
WHERE u.role IS NULL;

UPDATE `users`
SET role = 'ROLE_SUPERVISOR'
WHERE role IS NULL
  AND (FIND_IN_SET('ROLE_ADMINISTRATOR', REPLACE(roles, ' ', '')) > 0
       OR FIND_IN_SET('ROLE_SECURITY_MANAGER', REPLACE(roles, ' ', '')) > 0);

UPDATE `users`
SET role = 'ROLE_OPERATOR'
WHERE role IS NULL;

-- Former auditors keep a read-only profile: they cannot register entries or exits.
INSERT INTO `user_permissions` (`user_id`, `permission_code`, `granted`, `company_id`)
SELECT u.id, p.code, b'0', u.company_id
FROM users u
CROSS JOIN (SELECT 'inventory.entry.create' AS code UNION ALL SELECT 'inventory.exit.create') p
WHERE u.company_id IS NOT NULL
  AND u.role = 'ROLE_OPERATOR'
  AND FIND_IN_SET('ROLE_AUDITOR', REPLACE(u.roles, ' ', '')) > 0
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = u.id AND up.permission_code = p.code);

-- Owner of each organization.
UPDATE `organizations` o
JOIN (SELECT company_id, MIN(id) AS owner_id FROM users
      WHERE role = 'ROLE_OWNER' AND company_id IS NOT NULL GROUP BY company_id) owners
  ON owners.company_id = o.id
SET o.owner_user_id = owners.owner_id, o.updated_at = UTC_TIMESTAMP(6)
WHERE o.owner_user_id IS NULL;

-- -------------------------------------------------------------------------------------
-- 5. WAREHOUSE ASSIGNMENTS: users.warehouse_id -> user_warehouses
--    Only when the warehouse exists and belongs to the same company. users.warehouse_id is kept.
-- -------------------------------------------------------------------------------------
INSERT INTO `user_warehouses` (`user_id`, `warehouse_id`, `company_id`)
SELECT u.id, u.warehouse_id, u.company_id
FROM users u
JOIN warehouses w ON w.id = u.warehouse_id AND w.company_id = u.company_id
WHERE u.role <> 'ROLE_OWNER'
  AND NOT EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id AND uw.warehouse_id = u.warehouse_id);

-- -------------------------------------------------------------------------------------
-- 6. BACK-FILL company_id of stock movements (only empty values) from their warehouse.
--    subscriptions, profiles and incident_reports have no link to a company: their empty
--    company_id rows are listed in section 8 so you can assign them manually.
-- -------------------------------------------------------------------------------------
UPDATE `stock_movements` sm
JOIN warehouses w ON w.id = sm.warehouse_id
SET sm.company_id = w.company_id
WHERE sm.company_id IS NULL;

-- -------------------------------------------------------------------------------------
-- 7. FOREIGN KEYS to organizations on the existing tables (added only when absent and
--    when no row would violate them; otherwise a message is shown and nothing changes).
-- -------------------------------------------------------------------------------------
SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND CONSTRAINT_NAME = 'fk_users_organization');
SET @orphans := (SELECT COUNT(*) FROM users t LEFT JOIN organizations o ON o.id = t.company_id
                 WHERE t.company_id IS NOT NULL AND o.id IS NULL);
SET @sql := IF(@exists = 0 AND @orphans = 0,
               'ALTER TABLE `users` ADD CONSTRAINT `fk_users_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'warehouses' AND CONSTRAINT_NAME = 'fk_warehouses_organization');
SET @orphans := (SELECT COUNT(*) FROM warehouses t LEFT JOIN organizations o ON o.id = t.company_id WHERE o.id IS NULL);
SET @sql := IF(@exists = 0 AND @orphans = 0,
               'ALTER TABLE `warehouses` ADD CONSTRAINT `fk_warehouses_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND CONSTRAINT_NAME = 'fk_products_organization');
SET @orphans := (SELECT COUNT(*) FROM products t LEFT JOIN organizations o ON o.id = t.company_id WHERE o.id IS NULL);
SET @sql := IF(@exists = 0 AND @orphans = 0,
               'ALTER TABLE `products` ADD CONSTRAINT `fk_products_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'sensor_nodes' AND CONSTRAINT_NAME = 'fk_sensor_nodes_organization');
SET @orphans := (SELECT COUNT(*) FROM sensor_nodes t LEFT JOIN organizations o ON o.id = t.company_id WHERE o.id IS NULL);
SET @sql := IF(@exists = 0 AND @orphans = 0,
               'ALTER TABLE `sensor_nodes` ADD CONSTRAINT `fk_sensor_nodes_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'physical_events' AND CONSTRAINT_NAME = 'fk_physical_events_organization');
SET @orphans := (SELECT COUNT(*) FROM physical_events t LEFT JOIN organizations o ON o.id = t.company_id WHERE o.id IS NULL);
SET @sql := IF(@exists = 0 AND @orphans = 0,
               'ALTER TABLE `physical_events` ADD CONSTRAINT `fk_physical_events_organization` FOREIGN KEY (`company_id`) REFERENCES `organizations` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'organizations' AND CONSTRAINT_NAME = 'fk_organizations_owner_user');
SET @sql := IF(@exists = 0,
               'ALTER TABLE `organizations` ADD CONSTRAINT `fk_organizations_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)',
               'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------------------
-- 8. POST-CHECKS (read only)
-- -------------------------------------------------------------------------------------
SELECT o.id, o.name, o.owner_user_id, o.status,
       (SELECT COUNT(*) FROM users u WHERE u.company_id = o.id) AS users,
       (SELECT COUNT(*) FROM warehouses w WHERE w.company_id = o.id) AS warehouses
FROM organizations o ORDER BY o.id;

SELECT u.id, u.username, u.company_id, u.roles AS legacy_roles, u.role, u.status,
       (SELECT GROUP_CONCAT(uw.warehouse_id) FROM user_warehouses uw WHERE uw.user_id = u.id) AS warehouses
FROM users u ORDER BY u.id;

SELECT 'workers without assigned warehouse (assign them from Equipos y accesos)' AS post_check, COUNT(*) AS total
FROM users u
WHERE u.role <> 'ROLE_OWNER' AND NOT EXISTS (SELECT 1 FROM user_warehouses uw WHERE uw.user_id = u.id);

SELECT 'organizations without owner' AS post_check, COUNT(*) AS total FROM organizations WHERE owner_user_id IS NULL;

SELECT 'stock_movements with company_id different from their product' AS post_check, COUNT(*) AS total
FROM stock_movements sm JOIN products p ON p.id = sm.product_id
WHERE sm.company_id IS NOT NULL AND sm.company_id <> p.company_id;

SELECT 'rows without company_id' AS post_check, 'subscriptions' AS table_name, COUNT(*) AS total
FROM subscriptions WHERE company_id IS NULL
UNION ALL SELECT 'rows without company_id', 'profiles', COUNT(*) FROM profiles WHERE company_id IS NULL
UNION ALL SELECT 'rows without company_id', 'incident_reports', COUNT(*) FROM incident_reports WHERE company_id IS NULL
UNION ALL SELECT 'rows without company_id', 'stock_movements', COUNT(*) FROM stock_movements WHERE company_id IS NULL;

-- -------------------------------------------------------------------------------------
-- ROLLBACK (manual, NOT executed). Everything above is additive; to undo it:
--   ALTER TABLE organizations DROP FOREIGN KEY fk_organizations_owner_user;
--   ALTER TABLE users DROP FOREIGN KEY fk_users_organization;  (same for warehouses, products,
--   sensor_nodes, physical_events)
--   DROP TABLE user_permissions; DROP TABLE user_warehouses; DROP TABLE organizations;
--   ALTER TABLE users DROP COLUMN role, DROP COLUMN status, DROP COLUMN token_version;
--   ALTER TABLE stock_movements DROP COLUMN company_id; (same for subscriptions, profiles, incident_reports)
-- Or restore the backup exported before running this script.
-- -------------------------------------------------------------------------------------
