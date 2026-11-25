-- Structure copied from the FreeDB phpMyAdmin dump (freedb_vS3Zy07C.sql, MySQL 8.0.46) BEFORE the
-- multi-tenant migration, with SYNTHETIC data (no real accounts, emails or password hashes).
-- Used only by MigrationScriptIntegrationTest inside an isolated MySQL container.

CREATE TABLE `incident_reports` (
  `id` bigint NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `event_type` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `warehouse_name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `physical_events` (
  `id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `event_type` enum('DOOR_CLOSED','DOOR_OPENED','NODE_DISCONNECTED') NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `reconciled` bit(1) NOT NULL,
  `sensor_node_id` bigint NOT NULL,
  `zone_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `products` (
  `id` bigint NOT NULL,
  `category` varchar(30) NOT NULL,
  `company_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `current_stock` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `sku` varchar(32) NOT NULL,
  `unit` varchar(20) NOT NULL,
  `updated_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `profiles` (
  `id` bigint NOT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `security_alerts` (
  `id` bigint NOT NULL,
  `alert_type` varchar(30) NOT NULL,
  `justification` varchar(300) DEFAULT NULL,
  `message` varchar(255) NOT NULL,
  `physical_event_id` bigint DEFAULT NULL,
  `raised_at` datetime(6) NOT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` varchar(100) DEFAULT NULL,
  `severity` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `warehouse_id` bigint NOT NULL,
  `zone_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sensor_nodes` (
  `id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `connection_status` enum('OFFLINE','ONLINE') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `last_ping_at` datetime(6) DEFAULT NULL,
  `mac_address` varchar(17) NOT NULL,
  `model` varchar(100) NOT NULL,
  `sensor_type` enum('MAGNETIC_CONTACT','MOTION','RFIDReader','VIBRATION') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `zone_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `stock_movements` (
  `id` bigint NOT NULL,
  `justification` varchar(300) DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `performed_by` varchar(100) NOT NULL,
  `previous_stock` int NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `resulting_stock` int NOT NULL,
  `type` varchar(20) NOT NULL,
  `warehouse_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `subscriptions` (
  `id` bigint NOT NULL,
  `max_sensors` int NOT NULL,
  `max_warehouses` int NOT NULL,
  `plan_name` varchar(255) NOT NULL,
  `price` double NOT NULL,
  `status` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `company_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(120) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `roles` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(30) NOT NULL,
  `warehouse_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `warehouses` (
  `id` bigint NOT NULL,
  `city` varchar(100) NOT NULL,
  `company_id` bigint NOT NULL,
  `country` varchar(2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `district` varchar(100) NOT NULL,
  `name` varchar(80) NOT NULL,
  `status` enum('ACTIVE','SUSPENDED') NOT NULL,
  `street` varchar(160) NOT NULL,
  `updated_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `warehouse_zones` (
  `id` bigint NOT NULL,
  `code` varchar(30) NOT NULL,
  `name` varchar(80) NOT NULL,
  `opening_shift_end` varchar(5) NOT NULL,
  `opening_shift_start` varchar(5) NOT NULL,
  `risk_level` enum('CRITICAL','HIGH','LOW','MEDIUM') NOT NULL,
  `warehouse_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Synthetic data with the same shape as production: company 1 has every legacy role and
-- assignments to warehouses that no longer exist; company 2 has no administrator.
INSERT INTO `warehouses` (`id`, `city`, `company_id`, `country`, `created_at`, `district`, `name`, `status`, `street`, `updated_at`) VALUES
(1, 'VA', 1, 'US', '2026-11-25 03:07:36.062580', 'VA', 'Legacy Warehouse', 'ACTIVE', '11 Test Street', '2026-11-25 03:08:21.326136'),
(2, 'Lima', 2, 'PE', '2026-11-25 03:07:36.062580', 'Ate', 'Other Company Warehouse', 'ACTIVE', 'Av. Test 1', '2026-11-25 03:08:21.326136');

INSERT INTO `warehouse_zones` (`id`, `code`, `name`, `opening_shift_end`, `opening_shift_start`, `risk_level`, `warehouse_id`) VALUES
(1, 'ZONA 01', 'Zone One', '18:00', '08:00', 'LOW', 1);

INSERT INTO `products` (`id`, `category`, `company_id`, `created_at`, `created_by`, `current_stock`, `name`, `sku`, `unit`, `updated_at`) VALUES
(1, 'COMPUTING', 1, '2026-11-25 03:08:52.090117', NULL, 5, 'Computers', '1897', 'UNIT', '2026-11-25 03:09:03.965999');

INSERT INTO `stock_movements` (`id`, `justification`, `occurred_at`, `performed_by`, `previous_stock`, `product_id`, `quantity`, `resulting_stock`, `type`, `warehouse_id`) VALUES
(1, NULL, '2026-11-25 03:10:00.000000', 'legacy.keeper', 0, 1, 5, 5, 'ENTRY', 1);

INSERT INTO `users` (`id`, `company_id`, `created_at`, `email`, `full_name`, `password_hash`, `roles`, `updated_at`, `username`, `warehouse_id`) VALUES
(1, 1, '2026-11-13 05:54:32.832683', 'admin@legacy.test', 'Legacy Admin', 'not-a-real-hash', 'ROLE_ADMINISTRATOR', '2026-11-13 05:54:32.832683', 'legacy.admin', 4),
(2, 1, '2026-11-13 05:54:32.832683', 'manager@legacy.test', 'Legacy Manager', 'not-a-real-hash', 'ROLE_SECURITY_MANAGER', '2026-11-13 05:54:32.832683', 'legacy.manager', 3),
(3, 1, '2026-11-13 05:54:32.832683', 'keeper@legacy.test', 'Legacy Keeper', 'not-a-real-hash', 'ROLE_WAREHOUSE_KEEPER', '2026-11-13 05:54:32.832683', 'legacy.keeper', 1),
(4, 1, '2026-11-13 05:54:32.832683', 'auditor@legacy.test', 'Legacy Auditor', 'not-a-real-hash', 'ROLE_AUDITOR', '2026-11-13 05:54:32.832683', 'legacy.auditor', 1),
(5, 2, '2026-11-13 05:54:32.832683', 'keeper2@legacy.test', 'Other Keeper', 'not-a-real-hash', 'ROLE_WAREHOUSE_KEEPER', '2026-11-13 05:54:32.832683', 'other.keeper', 1),
(6, NULL, '2026-11-13 05:54:32.832683', 'orphan@legacy.test', 'No Company', 'not-a-real-hash', 'ROLE_AUDITOR', '2026-11-13 05:54:32.832683', 'no.company', NULL),
(7, 1, '2026-11-13 05:54:32.832683', 'admin2@legacy.test', 'Second Admin', 'not-a-real-hash', 'ROLE_ADMINISTRATOR', '2026-11-13 05:54:32.832683', 'second.admin', NULL);

ALTER TABLE `incident_reports` ADD PRIMARY KEY (`id`);
ALTER TABLE `physical_events` ADD PRIMARY KEY (`id`), ADD KEY `idx_physical_event_company_time` (`company_id`,`occurred_at`);
ALTER TABLE `products` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `uk_product_company_id_sku` (`company_id`,`sku`);
ALTER TABLE `profiles` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `UKlnk8iosvsrn5614xw3lgnybgk` (`email`);
ALTER TABLE `security_alerts` ADD PRIMARY KEY (`id`), ADD KEY `idx_security_alert_status` (`status`),
  ADD KEY `idx_security_alert_warehouse_id` (`warehouse_id`), ADD KEY `idx_security_alert_raised_at` (`raised_at`);
ALTER TABLE `sensor_nodes` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `uk_sensor_node_mac_address` (`mac_address`);
ALTER TABLE `stock_movements` ADD PRIMARY KEY (`id`), ADD KEY `idx_stock_movement_product_id` (`product_id`),
  ADD KEY `idx_stock_movement_warehouse_id` (`warehouse_id`), ADD KEY `idx_stock_movement_occurred_at` (`occurred_at`);
ALTER TABLE `subscriptions` ADD PRIMARY KEY (`id`);
ALTER TABLE `users` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `uk_users_username` (`username`), ADD UNIQUE KEY `uk_users_email` (`email`);
ALTER TABLE `warehouses` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `uk_warehouse_company_name` (`company_id`,`name`);
ALTER TABLE `warehouse_zones` ADD PRIMARY KEY (`id`), ADD UNIQUE KEY `uk_warehouse_zone_code` (`warehouse_id`,`code`);

ALTER TABLE `incident_reports` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `physical_events` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `products` MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
ALTER TABLE `profiles` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `security_alerts` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `sensor_nodes` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `stock_movements` MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
ALTER TABLE `subscriptions` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `users` MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;
ALTER TABLE `warehouses` MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;
ALTER TABLE `warehouse_zones` MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `warehouse_zones` ADD CONSTRAINT `FK4valahp0ts451fd3s411tjfe2` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`id`);
