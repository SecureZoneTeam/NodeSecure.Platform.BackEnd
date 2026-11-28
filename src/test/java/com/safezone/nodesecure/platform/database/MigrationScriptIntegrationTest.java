package com.safezone.nodesecure.platform.database;

import com.safezone.nodesecure.platform.NodeSecurePlatformApplication;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.support.MySqlTestContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies database/multitenant_rbac.sql (the script imported manually in FreeDB) on an isolated MySQL 8
 * schema that reproduces the FreeDB structure, twice, and checks that existing data is preserved and
 * correctly associated with its organization. Finally the application boots with ddl-auto=validate
 * on the migrated schema, proving that the script matches the JPA mappings used in production.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationScriptIntegrationTest {

    private static final String SCHEMA = "legacy_migration_it";
    private static final Path MIGRATION = Path.of("database", "multitenant_rbac.sql");
    /** T25..T31: imported after multitenant_rbac.sql. */
    private static final Path SAAS_MIGRATION = Path.of("database", "saas_traceability_reporting.sql");

    private JdbcTemplate jdbc;
    private DriverManagerDataSource dataSource;

    @BeforeAll
    void prepareLegacyDatabaseAndMigrateTwice() throws Exception {
        var mysql = MySqlTestContainer.get();
        try (Connection root = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            root.createStatement().execute("DROP DATABASE IF EXISTS " + SCHEMA);
            root.createStatement().execute("CREATE DATABASE " + SCHEMA
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        String url = MySqlTestContainer.jdbcUrlFor(SCHEMA);
        MySqlTestContainer.assertIsolated(url);
        dataSource = new DriverManagerDataSource(url, mysql.getUsername(), mysql.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        populate(new ClassPathResource("db/freedb_legacy_schema.sql"));
        populate(new FileSystemResource(MIGRATION));
        populate(new FileSystemResource(MIGRATION)); // re-import must not change anything
        // Rows created before T25..T31 must survive the second script untouched.
        jdbc.update("INSERT INTO subscriptions (id, company_id, max_sensors, max_warehouses, plan_name, price, status) "
                + "VALUES (50, 1, 10, 2, 'Legacy plan', 99.5, 'ACTIVE')");
        jdbc.update("INSERT INTO incident_reports (id, company_id, description, event_type, status, timestamp, warehouse_name) "
                + "VALUES (60, 1, 'Manual report', 'DISCREPANCY', 'OPEN', '2026-10-01 10:00:00', 'Central')");
        populate(new FileSystemResource(SAAS_MIGRATION));
        populate(new FileSystemResource(SAAS_MIGRATION)); // re-import must not change anything
    }

    private void populate(org.springframework.core.io.Resource script) {
        var populator = new ResourceDatabasePopulator(script);
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
    }

    @Test
    void createsOneOrganizationPerExistingCompanyKeepingItsId() {
        List<Map<String, Object>> organizations = jdbc.queryForList(
                "SELECT id, owner_user_id, status FROM organizations ORDER BY id");
        assertThat(organizations).hasSize(2);
        assertThat(organizations.get(0).get("id")).isEqualTo(1L);
        assertThat(organizations.get(0).get("owner_user_id")).isEqualTo(1L);
        assertThat(organizations.get(1).get("id")).isEqualTo(2L);
        assertThat(organizations.get(1).get("owner_user_id")).isNull();
        assertThat(organizations).allSatisfy(row -> assertThat(row.get("status")).isEqualTo("ACTIVE"));
    }

    @Test
    void mapsLegacyRolesWithoutOverwritingTheLegacyColumn() {
        Map<Long, String> roles = Map.of(
                1L, "ROLE_OWNER", 7L, "ROLE_SUPERVISOR", 2L, "ROLE_SUPERVISOR", 3L, "ROLE_OPERATOR",
                4L, "ROLE_OPERATOR", 5L, "ROLE_OPERATOR", 6L, "ROLE_OPERATOR");
        roles.forEach((id, role) -> assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, id))
                .as("user %s", id).isEqualTo(role));
        assertThat(jdbc.queryForObject("SELECT roles FROM users WHERE id = 1", String.class)).isEqualTo("ROLE_ADMINISTRATOR");
        assertThat(jdbc.queryForObject("SELECT roles FROM users WHERE id = 4", String.class)).isEqualTo("ROLE_AUDITOR");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE' AND token_version = 0", Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(7);
    }

    @Test
    void formerAuditorsBecomeReadOnlyOperators() {
        assertThat(jdbc.queryForList("SELECT permission_code FROM user_permissions WHERE user_id = 4 AND granted = b'0' "
                + "ORDER BY permission_code", String.class))
                .containsExactly("inventory.entry.create", "inventory.exit.create");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_permissions", Integer.class)).isEqualTo(2);
    }

    @Test
    void migratesOnlyValidSameCompanyWarehouseAssignments() {
        assertThat(jdbc.queryForList("SELECT CONCAT(user_id, ':', warehouse_id, ':', company_id) FROM user_warehouses "
                + "ORDER BY user_id", String.class))
                .containsExactly("3:1:1", "4:1:1");
        // Legacy column kept as it was, including the dangling and cross-company values.
        assertThat(jdbc.queryForObject("SELECT warehouse_id FROM users WHERE id = 5", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT warehouse_id FROM users WHERE id = 1", Long.class)).isEqualTo(4L);
    }

    @Test
    void backFillsTheCompanyOfExistingStockMovements() {
        assertThat(jdbc.queryForObject("SELECT company_id FROM stock_movements WHERE id = 1", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT performed_by FROM stock_movements WHERE id = 1", String.class))
                .isEqualTo("legacy.keeper");
    }

    @Test
    void addsForeignKeysToOrganizations() {
        assertThat(jdbc.queryForList("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME LIKE 'fk\\_%'",
                String.class))
                .contains("fk_users_organization", "fk_warehouses_organization", "fk_products_organization",
                        "fk_sensor_nodes_organization", "fk_physical_events_organization", "fk_organizations_owner_user",
                        "fk_user_warehouses_user", "fk_user_permissions_user");
    }

    @Test
    void seedsThePlanCatalogOnlyOnce() {
        assertThat(jdbc.queryForList("SELECT CONCAT(code, ':', monthly_price, ':', warehouse_limit, ':', sensor_node_limit, ':', user_limit) "
                + "FROM subscription_plans ORDER BY monthly_price", String.class))
                .containsExactly("BASIC:49.00:1:5:3", "PREMIUM:129.00:5:30:15", "CORPORATE:349.00:25:200:100");
    }

    @Test
    void keepsExistingSubscriptionsAndReportsUntouched() {
        Map<String, Object> subscription = jdbc.queryForMap("SELECT plan_name, price, status, plan_id, started_at FROM subscriptions WHERE id = 50");
        assertThat(subscription.get("plan_name")).isEqualTo("Legacy plan");
        assertThat(subscription.get("price")).isEqualTo(99.5);
        assertThat(subscription.get("status")).isEqualTo("ACTIVE");
        assertThat(subscription.get("plan_id")).isNull();
        assertThat(subscription.get("started_at")).isNull();
        Map<String, Object> report = jdbc.queryForMap("SELECT description, source, warehouse_id FROM incident_reports WHERE id = 60");
        assertThat(report.get("description")).isEqualTo("Manual report");
        assertThat(report.get("source")).isNull();
        assertThat(report.get("warehouse_id")).isNull();
    }

    @Test
    void createsTheConstraintsThatPreventDuplicates() {
        assertThat(jdbc.queryForList("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_TYPE IN ('UNIQUE', 'FOREIGN KEY')", String.class))
                .contains("uk_companies_tax_id", "uk_companies_company_id", "uk_subscription_plans_code",
                        "uk_invoices_subscription_period", "uk_incident_reports_period", "fk_companies_organization",
                        "fk_subscriptions_plan", "fk_invoices_subscription", "fk_invoices_plan",
                        "fk_traceability_records_organization");
    }

    @Test
    void applicationStartsWithDdlValidateOnTheMigratedSchema() {
        // Command-line arguments have the highest precedence, so no profile can point elsewhere.
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(NodeSecurePlatformApplication.class)
                .run("--spring.datasource.url=" + MySqlTestContainer.jdbcUrlFor(SCHEMA),
                        "--spring.datasource.username=" + MySqlTestContainer.get().getUsername(),
                        "--spring.datasource.password=" + MySqlTestContainer.get().getPassword(),
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--iam.seed-demo-data=false",
                        "--server.port=0")) {
            UserRepository users = context.getBean(UserRepository.class);

            User owner = users.findById(1L).orElseThrow();
            assertThat(owner.getRole()).isEqualTo(Role.ROLE_OWNER);
            assertThat(owner.getCompanyId()).isEqualTo(1L);

            User keeper = users.findById(3L).orElseThrow();
            assertThat(keeper.getWarehouseIds()).containsExactly(1L);
            assertThat(keeper.effectivePermissions()).contains(Permission.ENTRY_CREATE);

            User auditor = users.findById(4L).orElseThrow();
            assertThat(auditor.effectivePermissions()).doesNotContain(Permission.ENTRY_CREATE, Permission.EXIT_CREATE)
                    .contains(Permission.INVENTORY_VIEW);

            assertThat(context.getBean(com.safezone.nodesecure.platform.subscriptions.domain.repositories.SubscriptionPlanRepository.class)
                    .findAllByActiveTrueOrderByMonthlyPriceAsc()).hasSize(3);

            assertThat(users.findAllByCompanyId(2L)).extracting(user -> user.getUsername().value())
                    .containsExactly("other.keeper");
        }
    }
}
