package com.safezone.nodesecure.platform.saas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safezone.nodesecure.platform.support.MySqlTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * End-to-end checks of T25..T31 through the REST API, JWT and permissions, against an isolated
 * MySQL 8 container (never FreeDB): companies, plans, subscriptions, invoices, traceability log,
 * its filters and incident reports, including isolation between organizations.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "iam.seed-demo-data=false"
})
@AutoConfigureMockMvc
class SaasTraceabilityIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        var mysql = MySqlTestContainer.get();
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private String suffix;

    private record Session(String token, long userId, long organizationId) {
    }

    @BeforeEach
    void isolationGuard() throws Exception {
        try (var connection = dataSource.getConnection()) {
            MySqlTestContainer.assertIsolated(connection.getMetaData().getURL());
        }
        jdbc = new JdbcTemplate(dataSource);
        suffix = UUID.randomUUID().toString().substring(0, 6);
    }

    // ------------------------------------------------------------------ T25 companies

    @Test
    void companiesAreRegisteredWithAValidUniqueRucAndStayInsideTheirOrganization() throws Exception {
        Session a = subscribed(signUp("compa" + suffix));
        Session b = signUp("compb" + suffix);
        String ruc = validRuc();

        call(post("/api/v1/companies").content(companyJson("20512345678")), a, 400); // wrong check digit
        call(post("/api/v1/companies").content(companyJson("123")), a, 400);
        JsonNode company = body(call(post("/api/v1/companies").content(companyJson(ruc)), a, 201));
        assertThat(company.get("taxId").asText()).isEqualTo(ruc);
        assertThat(company.get("organizationId").asLong()).isEqualTo(a.organizationId());
        assertThat(company.get("country").asText()).isEqualTo("PE");
        long companyId = company.get("id").asLong();

        call(post("/api/v1/companies").content(companyJson(validRuc())), a, 409); // one company per organization
        call(post("/api/v1/companies").content(companyJson(ruc)), b, 409);        // RUC unique in the platform
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM companies WHERE tax_id = ?", Integer.class, ruc)).isEqualTo(1);

        assertThat(ids(call(get("/api/v1/companies"), a, 200))).containsExactly(companyId);
        assertThat(ids(call(get("/api/v1/companies?taxId=" + ruc), a, 200))).containsExactly(companyId);
        assertThat(body(call(get("/api/v1/companies?taxId=" + ruc), b, 200))).isEmpty(); // no cross-tenant lookup
        call(get("/api/v1/companies?taxId=999"), a, 400);
        call(get("/api/v1/companies/" + companyId), a, 200);
        call(get("/api/v1/companies/" + companyId), b, 404);

        long w1 = createWarehouse(a, "W1");
        Session worker = signIn(createWorker(a, "cw" + suffix, "ROLE_SUPERVISOR", List.of(w1)));
        call(get("/api/v1/companies"), worker, 403);
        call(post("/api/v1/companies").content(companyJson(validRuc())), worker, 403);
        assertThat(mvc.perform(get("/api/v1/companies")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ------------------------------------------------------------------ T26 plans

    @Test
    void planCatalogIsPublicWithPricesAndLimits() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/subscription-plans")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode plans = body(result);
        List<String> codes = new ArrayList<>();
        plans.forEach(plan -> codes.add(plan.get("code").asText()));
        assertThat(codes).containsExactly("BASIC", "PREMIUM", "CORPORATE");
        JsonNode basic = plans.get(0);
        assertThat(basic.get("name").asText()).isEqualTo("Básico");
        assertThat(basic.get("monthlyPrice").decimalValue()).isEqualByComparingTo("49.00");
        assertThat(basic.get("currency").asText()).isEqualTo("USD");
        assertThat(basic.get("warehouseLimit").asInt()).isEqualTo(1);
        assertThat(basic.get("sensorNodeLimit").asInt()).isEqualTo(5);
        assertThat(basic.get("userLimit").asInt()).isEqualTo(3);

        assertThat(mvc.perform(get("/api/v1/subscription-plans/" + basic.get("id").asLong())).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(mvc.perform(get("/api/v1/subscription-plans/999999")).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mvc.perform(post("/api/v1/subscription-plans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus()).isIn(401, 405);
    }

    // ------------------------------------------------------------------ T27 + T28 subscriptions and invoices

    @Test
    void subscriptionLifecycleIssuesInvoicesAndValidatesTransitionsAndLimits() throws Exception {
        Session a = signUp("suba" + suffix);
        Session b = signUp("subb" + suffix);

        call(post("/api/v1/subscriptions").content("{}"), a, 400);
        call(post("/api/v1/subscriptions").content("{\"planCode\":\"GOLD\"}"), a, 404);
        call(post("/api/v1/subscriptions").content("{\"planCode\":\"BASIC\",\"status\":\"CANCELLED\"}"), a, 400);

        JsonNode activated = body(call(post("/api/v1/subscriptions").content("{\"planCode\":\"BASIC\"}"), a, 201));
        long subscriptionId = activated.get("id").asLong();
        assertThat(activated.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(activated.get("planName").asText()).isEqualTo("Básico");
        assertThat(activated.get("price").asDouble()).isEqualTo(49.0);
        assertThat(activated.get("companyId").asLong()).isEqualTo(a.organizationId());
        assertThat(activated.get("startedAt").isNull()).isFalse();
        assertThat(activated.get("latestInvoiceId").isNull()).isFalse();

        call(post("/api/v1/subscriptions").content("{\"planCode\":\"PREMIUM\"}"), a, 409); // already active
        call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan").content("{\"planCode\":\"BASIC\"}"), a, 409);

        JsonNode changed = body(call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan")
                .content("{\"planCode\":\"PREMIUM\"}"), a, 200));
        assertThat(changed.get("planName").asText()).isEqualTo("Premium");
        assertThat(changed.get("maxWarehouses").asInt()).isEqualTo(5);

        // Downgrade rejected while the organization uses more than the plan allows (2 warehouses > 1).
        createWarehouse(a, "W1");
        createWarehouse(a, "W2");
        call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan").content("{\"planCode\":\"BASIC\"}"), a, 409);

        // Organization B cannot see or operate the subscription of A.
        call(get("/api/v1/subscriptions/" + subscriptionId), b, 404);
        call(post("/api/v1/subscriptions/" + subscriptionId + "/cancellation"), b, 404);
        assertThat(body(call(get("/api/v1/subscriptions"), b, 200))).isEmpty();
        call(get("/api/v1/subscriptions/current"), b, 404);

        // T28: one invoice per activation / plan change, with amount, currency, dates and status.
        JsonNode invoices = body(call(get("/api/v1/invoices"), a, 200));
        assertThat(invoices).hasSize(2);
        assertThat(invoices.get(0).get("concept").asText()).isEqualTo("PLAN_CHANGE");
        assertThat(invoices.get(0).get("amount").decimalValue()).isEqualByComparingTo("129.00");
        assertThat(invoices.get(1).get("concept").asText()).isEqualTo("ACTIVATION");
        assertThat(invoices.get(1).get("amount").decimalValue()).isEqualByComparingTo("49.00");
        invoices.forEach(invoice -> {
            assertThat(invoice.get("currency").asText()).isEqualTo("USD");
            assertThat(invoice.get("status").asText()).isEqualTo("ISSUED");
            assertThat(invoice.get("subscriptionId").asLong()).isEqualTo(subscriptionId);
            assertThat(invoice.get("companyId").asLong()).isEqualTo(a.organizationId());
            assertThat(invoice.get("periodEnd").asText()).isGreaterThan(invoice.get("periodStart").asText());
        });
        long invoiceId = invoices.get(0).get("id").asLong();
        call(get("/api/v1/invoices/" + invoiceId), a, 200);
        call(get("/api/v1/invoices/" + invoiceId), b, 404);
        assertThat(body(call(get("/api/v1/invoices"), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/invoices?subscriptionId=" + subscriptionId), a, 200))).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE subscription_id = ?", Integer.class,
                subscriptionId)).isEqualTo(2);
        // The database itself rejects a second invoice for the same subscription period.
        assertThatThrownBy(() -> jdbc.update("INSERT INTO invoices (company_id, subscription_id, plan_id, concept, amount, "
                + "currency, status, issued_at, period_start, period_end) SELECT company_id, subscription_id, plan_id, concept, "
                + "amount, currency, status, issued_at, period_start, period_end FROM invoices WHERE id = ?", invoiceId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Cancellation is final; a new subscription can then be activated.
        JsonNode cancelled = body(call(post("/api/v1/subscriptions/" + subscriptionId + "/cancellation"), a, 200));
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("cancelledAt").isNull()).isFalse();
        call(post("/api/v1/subscriptions/" + subscriptionId + "/cancellation"), a, 409);
        call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan").content("{\"planCode\":\"CORPORATE\"}"), a, 409);
        call(get("/api/v1/subscriptions/current"), a, 404);
        call(post("/api/v1/subscriptions").content("{\"planCode\":\"BASIC\"}"), a, 409); // usage still exceeds BASIC
        JsonNode reactivated = body(call(post("/api/v1/subscriptions").content("{\"planCode\":\"CORPORATE\"}"), a, 201));
        assertThat(body(call(get("/api/v1/subscriptions/current"), a, 200)).get("id").asLong())
                .isEqualTo(reactivated.get("id").asLong());
        assertThat(body(call(get("/api/v1/invoices"), a, 200))).hasSize(3);
    }

    @Test
    void legacyFrontendPayloadUsesCatalogPriceAndWorkersCannotManageBilling() throws Exception {
        Session owner = signUp("legacy" + suffix);
        JsonNode created = body(call(post("/api/v1/subscriptions").content("{\"planName\":\"Premium\",\"price\":1,"
                + "\"maxWarehouses\":999,\"maxSensors\":999,\"status\":\"ACTIVE\"}"), owner, 201));
        assertThat(created.get("price").asDouble()).isEqualTo(129.0);
        assertThat(created.get("maxWarehouses").asInt()).isEqualTo(5);
        assertThat(created.get("maxSensors").asInt()).isEqualTo(30);

        long w1 = createWarehouse(owner, "W1");
        Session worker = signIn(createWorker(owner, "bw" + suffix, "ROLE_SUPERVISOR", List.of(w1)));
        call(get("/api/v1/subscriptions"), worker, 403);
        call(get("/api/v1/invoices"), worker, 403);
        call(post("/api/v1/subscriptions/" + created.get("id").asLong() + "/cancellation"), worker, 403);
        assertThat(mvc.perform(get("/api/v1/invoices")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ------------------------------------------------------------------ T29 + T30 traceability

    @Test
    void domainEventsAreRecordedImmutablyAndCanBeFilteredAndPaged() throws Exception {
        Session a = subscribed(signUp("tra" + suffix));
        Session b = subscribed(signUp("trb" + suffix));
        long w1 = createWarehouse(a, "W1");
        long w2 = createWarehouse(a, "W2");
        long wb = createWarehouse(b, "WB");
        String workerName = "tw" + suffix;
        createWorker(a, workerName, "ROLE_OPERATOR", List.of(w1));
        long product = createProduct(a, "SKU-T-" + suffix.toUpperCase());
        movement(a, product, w1, "ENTRY", 10);
        movement(a, product, w1, "RECONCILIATION", 8);
        movement(a, product, w2, "ENTRY", 4);
        long alertId = body(call(post("/api/v1/security-alerts").content("{\"warehouseId\":" + w2
                + ",\"alertType\":\"UNAUTHORIZED_ACCESS\",\"message\":\"Door forced\"}"), a, 201)).get("id").asLong();
        call(post("/api/v1/security-alerts/" + alertId + "/false-alarm")
                .content("{\"justification\":\"Scheduled maintenance\",\"resolvedBy\":\"Guard\"}"), a, 200);

        // T29: users, inventory and alerts events were persisted with organization, actor and time.
        JsonNode all = body(call(get("/api/v1/traceability-records?size=100"), a, 200));
        List<String> events = new ArrayList<>();
        all.get("content").forEach(record -> events.add(record.get("eventType").asText()));
        assertThat(events).contains("USER_REGISTERED", "PRODUCT_CREATED", "STOCK_ENTRY", "STOCK_RECONCILIATION",
                "SECURITY_ALERT_RAISED", "SECURITY_ALERT_FALSE_ALARM");
        all.get("content").forEach(record -> {
            assertThat(record.get("companyId").asLong()).isEqualTo(a.organizationId());
            assertThat(record.get("occurredAt").isNull()).isFalse();
            assertThat(record.get("description").asText()).doesNotContain("Secret-123", "@nodesecure.test");
        });
        JsonNode reconciliation = first(all, "STOCK_RECONCILIATION");
        assertThat(reconciliation.get("actor").asText()).isEqualTo("tra" + suffix);
        assertThat(reconciliation.get("warehouseId").asLong()).isEqualTo(w1);
        assertThat(reconciliation.get("recordType").asText()).isEqualTo("DISCREPANCY");
        assertThat(first(all, "USER_REGISTERED").get("entityId").asText()).isIn("tra" + suffix, workerName);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM traceability_records WHERE company_id = ?", Integer.class,
                a.organizationId())).isEqualTo(all.get("totalElements").asInt());

        // Immutable through the API: no update or delete.
        long recordId = reconciliation.get("id").asLong();
        call(delete("/api/v1/traceability-records/" + recordId), a, 405);
        call(put("/api/v1/traceability-records/" + recordId).content("{}"), a, 405);
        call(post("/api/v1/traceability-records").content("{}"), a, 405);
        assertThat(body(call(get("/api/v1/traceability-records/" + recordId), a, 200)).get("description").asText())
                .isEqualTo(reconciliation.get("description").asText());

        // T30: combinable filters.
        assertThat(eventTypes(call(get("/api/v1/traceability-records?warehouseId=" + w1), a, 200)))
                .containsOnly("STOCK_ENTRY", "STOCK_RECONCILIATION");
        assertThat(eventTypes(call(get("/api/v1/traceability-records?recordType=DISCREPANCY"), a, 200)))
                .containsExactly("STOCK_RECONCILIATION");
        assertThat(eventTypes(call(get("/api/v1/traceability-records?recordType=SECURITY_ALERT&recordType=DISCREPANCY"
                + "&warehouseId=" + w2), a, 200)))
                .containsExactlyInAnyOrder("SECURITY_ALERT_RAISED", "SECURITY_ALERT_FALSE_ALARM");
        assertThat(eventTypes(call(get("/api/v1/traceability-records?eventType=STOCK_ENTRY&warehouseId=" + w2), a, 200)))
                .containsExactly("STOCK_ENTRY");
        String occurred = reconciliation.get("occurredAt").asText();
        assertThat(eventTypes(call(get("/api/v1/traceability-records?recordType=DISCREPANCY&from=" + occurred), a, 200)))
                .containsExactly("STOCK_RECONCILIATION");
        assertThat(body(call(get("/api/v1/traceability-records?recordType=DISCREPANCY&to=" + occurred), a, 200))
                .get("content")).isEmpty();
        assertThat(body(call(get("/api/v1/traceability-records?from=2020-01-01T00:00:00Z&to=2020-02-01T00:00:00Z"), a, 200))
                .get("totalElements").asInt()).isZero();

        // Pagination.
        JsonNode page = body(call(get("/api/v1/traceability-records?warehouseId=" + w1 + "&size=1&page=1&direction=asc"), a, 200));
        assertThat(page.get("content")).hasSize(1);
        assertThat(page.get("totalElements").asInt()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("content").get(0).get("eventType").asText()).isEqualTo("STOCK_RECONCILIATION");

        // Validation.
        call(get("/api/v1/traceability-records?recordType=MERMA"), a, 400);
        call(get("/api/v1/traceability-records?from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z"), a, 400);
        call(get("/api/v1/traceability-records?size=1000"), a, 400);
        call(get("/api/v1/traceability-records?from=yesterday"), a, 400);

        // Isolation between organizations and warehouse scope of workers.
        assertThat(eventTypes(call(get("/api/v1/traceability-records"), b, 200))).containsOnly("USER_REGISTERED");
        call(get("/api/v1/traceability-records?warehouseId=" + w1), b, 404);
        call(get("/api/v1/traceability-records/" + recordId), b, 404);
        call(get("/api/v1/traceability-records?warehouseId=" + wb), a, 404);
        Session worker = signIn(workerName);
        JsonNode workerView = body(call(get("/api/v1/traceability-records?size=100"), worker, 200));
        workerView.get("content").forEach(record -> assertThat(record.get("warehouseId").asLong()).isEqualTo(w1));
        assertThat(workerView.get("totalElements").asInt()).isEqualTo(2);
        call(get("/api/v1/traceability-records?warehouseId=" + w2), worker, 403);
        assertThat(mvc.perform(get("/api/v1/traceability-records")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ------------------------------------------------------------------ T31 incident reports

    @Test
    void monthlyIncidentReportIsDerivedFromTheLogWithoutDuplicates() throws Exception {
        Session a = subscribed(signUp("rpa" + suffix));
        Session b = subscribed(signUp("rpb" + suffix));
        long w1 = createWarehouse(a, "W1");
        long w2 = createWarehouse(a, "W2");
        long product = createProduct(a, "SKU-R-" + suffix.toUpperCase());
        movement(a, product, w1, "ENTRY", 10);
        movement(a, product, w1, "RECONCILIATION", 9);
        call(post("/api/v1/security-alerts").content("{\"warehouseId\":" + w1
                + ",\"alertType\":\"NODE_DISCONNECTION\",\"message\":\"Node offline\"}"), a, 201);

        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        String currentMonth = reportJson(w1, current);
        JsonNode report = body(call(post("/api/v1/reporting/incident-reports").content(currentMonth), a, 201));
        long reportId = report.get("id").asLong();
        assertThat(report.get("source").asText()).isEqualTo("TRACEABILITY");
        assertThat(report.get("warehouseId").asLong()).isEqualTo(w1);
        assertThat(report.get("warehouseName").asText()).startsWith("W1");
        assertThat(report.get("discrepancyCount").asInt()).isEqualTo(1);
        assertThat(report.get("disconnectionCount").asInt()).isEqualTo(1);
        assertThat(report.get("securityAlertCount").asInt()).isZero();
        assertThat(report.get("incidentCount").asInt()).isEqualTo(2);

        // Same warehouse and month: no duplicate; the open month is refreshed with new events.
        movement(a, product, w1, "RECONCILIATION", 7);
        JsonNode again = body(call(post("/api/v1/reporting/incident-reports").content(currentMonth), a, 200));
        assertThat(again.get("id").asLong()).isEqualTo(reportId);
        assertThat(again.get("discrepancyCount").asInt()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incident_reports WHERE company_id = ? AND warehouse_id = ?",
                Integer.class, a.organizationId(), w1)).isEqualTo(1);

        // A closed month without incidents; generated twice it is still one report.
        String previousMonth = reportJson(w2, current.minusMonths(1));
        JsonNode empty = body(call(post("/api/v1/reporting/incident-reports").content(previousMonth), a, 201));
        assertThat(empty.get("description").asText()).isEqualTo("Cero Incidentes Registrados");
        assertThat(body(call(post("/api/v1/reporting/incident-reports").content(previousMonth), a, 200)).get("id").asLong())
                .isEqualTo(empty.get("id").asLong());

        // Validation, scope and isolation.
        call(post("/api/v1/reporting/incident-reports").content(reportJson(w1, current.plusMonths(1))), a, 400);
        call(post("/api/v1/reporting/incident-reports").content("{\"warehouseId\":" + w1 + ",\"year\":2026,\"month\":13}"), a, 400);
        call(post("/api/v1/reporting/incident-reports").content(currentMonth), b, 404);
        call(get("/api/v1/reporting/" + reportId), b, 404);
        call(get("/api/v1/reporting/" + reportId + "/export"), b, 404);
        Session supervisor = signIn(createWorker(a, "rs" + suffix, "ROLE_SUPERVISOR", List.of(w2)));
        call(post("/api/v1/reporting/incident-reports").content(currentMonth), supervisor, 403);
        call(get("/api/v1/reporting/" + reportId), supervisor, 404);

        // Persisted reports are listed together with manual ones (existing endpoint unchanged).
        call(post("/api/v1/reporting").content("{\"warehouseName\":\"Central\",\"eventType\":\"DISCREPANCY\","
                + "\"status\":\"OPEN\",\"description\":\"Manual\"}"), a, 201);
        assertThat(body(call(get("/api/v1/reporting"), a, 200))).hasSize(3);
        assertThat(body(call(get("/api/v1/reporting"), b, 200))).isEmpty();

        // Export (CSV for Excel) with the incidents behind the report.
        MvcResult csv = call(get("/api/v1/reporting/" + reportId + "/export"), a, 200);
        assertThat(csv.getResponse().getContentType()).startsWith("text/csv");
        String content = csv.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(content).contains("STOCK_RECONCILIATION", "SECURITY_ALERT_RAISED", "Desconexiones de nodo");
        assertThat(call(get("/api/v1/reporting/" + empty.get("id").asLong() + "/export"), a, 200).getResponse()
                .getContentAsString(StandardCharsets.UTF_8)).contains("Cero Incidentes Registrados");
    }

    // ------------------------------------------------------------------ helpers

    private static String validRuc() {
        int[] weights = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        StringBuilder ruc = new StringBuilder("20");
        for (int i = 0; i < 8; i++) ruc.append(ThreadLocalRandom.current().nextInt(10));
        int sum = 0;
        for (int i = 0; i < 10; i++) sum += (ruc.charAt(i) - '0') * weights[i];
        int check = 11 - (sum % 11);
        return ruc.append(check == 10 ? 0 : check == 11 ? 1 : check).toString();
    }

    private String companyJson(String ruc) {
        return "{\"legalName\":\"Empresa " + suffix + " S.A.C.\",\"taxId\":\"" + ruc
                + "\",\"contactEmail\":\"contacto" + suffix + "@empresa.pe\"}";
    }

    private static String reportJson(long warehouseId, YearMonth month) {
        return "{\"warehouseId\":" + warehouseId + ",\"year\":" + month.getYear() + ",\"month\":" + month.getMonthValue() + "}";
    }

    private Session signUp(String username) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Owner " + username);
        body.put("email", username + "@nodesecure.test");
        body.put("username", username);
        body.put("password", "Secret-123");
        body.put("organizationName", "Org " + username);
        MvcResult result = mvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode node = body(result);
        return new Session(node.get("token").asText(), node.get("id").asLong(), node.get("organizationId").asLong());
    }

    /** Warehouses, sensors and users can only be created with an active subscription. */
    private Session subscribed(Session owner) throws Exception {
        call(post("/api/v1/subscriptions").content("{\"planCode\":\"PREMIUM\"}"), owner, 201);
        return owner;
    }

    private Session signIn(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Secret-123\"}")).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode node = body(result);
        return new Session(node.get("token").asText(), node.get("id").asLong(), node.get("organizationId").asLong());
    }

    private long createWarehouse(Session owner, String name) throws Exception {
        return body(call(post("/api/v1/warehouses").content("{\"name\":\"" + name + " " + suffix
                + "\",\"street\":\"Av. Test 123\",\"district\":\"Ate\",\"city\":\"Lima\",\"country\":\"PE\"}"), owner, 201))
                .get("id").asLong();
    }

    private String createWorker(Session owner, String username, String role, List<Long> warehouses) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Worker " + username);
        body.put("email", username + "@nodesecure.test");
        body.put("username", username);
        body.put("password", "Secret-123");
        body.put("role", role);
        body.put("warehouseIds", warehouses);
        call(post("/api/v1/users").content(json.writeValueAsString(body)), owner, 201);
        return username;
    }

    private long createProduct(Session owner, String sku) throws Exception {
        return body(call(post("/api/v1/products").content("{\"name\":\"Product " + sku + "\",\"sku\":\"" + sku
                + "\",\"category\":\"NETWORKING\",\"unit\":\"UNIT\"}"), owner, 201)).get("id").asLong();
    }

    private void movement(Session session, long productId, long warehouseId, String type, int quantity) throws Exception {
        String justification = "RECONCILIATION".equals(type) ? ",\"justification\":\"Physical count\"" : "";
        call(post("/api/v1/stock-movements").content("{\"productId\":" + productId + ",\"warehouseId\":" + warehouseId
                + ",\"type\":\"" + type + "\",\"quantity\":" + quantity + justification + "}"), session, 201);
    }

    private MvcResult call(MockHttpServletRequestBuilder request, Session session, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + session.token())).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("%s %s -> %s", result.getRequest().getMethod(), result.getRequest().getRequestURI(),
                        result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        return result;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private List<Long> ids(MvcResult result) throws Exception {
        List<Long> ids = new ArrayList<>();
        body(result).forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }

    private List<String> eventTypes(MvcResult result) throws Exception {
        List<String> types = new ArrayList<>();
        body(result).get("content").forEach(node -> types.add(node.get("eventType").asText()));
        return types;
    }

    private static JsonNode first(JsonNode page, String eventType) {
        for (JsonNode record : page.get("content")) {
            if (eventType.equals(record.get("eventType").asText())) return record;
        }
        throw new AssertionError("No record " + eventType);
    }
}
