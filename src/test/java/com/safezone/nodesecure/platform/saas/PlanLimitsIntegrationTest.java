package com.safezone.nodesecure.platform.saas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safezone.nodesecure.platform.support.MySqlTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Plan limits enforced by the backend (warehouses, IoT sensors, users), mandatory subscription,
 * usage endpoint, downgrade checks and the self-service profile, against an isolated MySQL 8
 * container (never FreeDB).
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "iam.seed-demo-data=false"
})
@AutoConfigureMockMvc
class PlanLimitsIntegrationTest {

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

    private String suffix;

    private record Session(String token, long userId, long organizationId) {
    }

    @BeforeEach
    void isolationGuard() throws Exception {
        try (var connection = dataSource.getConnection()) {
            MySqlTestContainer.assertIsolated(connection.getMetaData().getURL());
        }
        suffix = UUID.randomUUID().toString().substring(0, 6);
    }

    @Test
    void limitedResourcesRequireAnActiveSubscription() throws Exception {
        Session owner = signUp("nosub" + suffix);

        JsonNode problem = body(call(post("/api/v1/warehouses").content(warehouseJson("W1")), owner, 409));
        assertThat(problem.get("code").asText()).isEqualTo("SUBSCRIPTION_REQUIRED");
        assertThat(problem.get("resource").asText()).isEqualTo("warehouses");
        call(get("/api/v1/subscriptions/current/usage"), owner, 404);

        // The screens needed to solve it stay reachable.
        assertThat(body(call(get("/api/v1/subscription-plans"), owner, 200))).hasSize(3);
        call(get("/api/v1/subscriptions"), owner, 200);
        call(get("/api/v1/profile"), owner, 200);
    }

    @Test
    void basicPlanLimitsAreEnforcedWithAStructuredError() throws Exception {
        Session owner = signUp("basic" + suffix);
        long subscriptionId = body(call(post("/api/v1/subscriptions").content("{\"planCode\":\"BASIC\"}"), owner, 201))
                .get("id").asLong();

        // Warehouses: 1
        long w1 = createWarehouse(owner, "W1");
        JsonNode warehouseLimit = body(call(post("/api/v1/warehouses").content(warehouseJson("W2")), owner, 409));
        assertThat(warehouseLimit.get("code").asText()).isEqualTo("PLAN_LIMIT_REACHED");
        assertThat(warehouseLimit.get("resource").asText()).isEqualTo("warehouses");
        assertThat(warehouseLimit.get("used").asLong()).isEqualTo(1);
        assertThat(warehouseLimit.get("limit").asInt()).isEqualTo(1);
        assertThat(warehouseLimit.get("planCode").asText()).isEqualTo("BASIC");
        assertThat(warehouseLimit.get("planName").asText()).isEqualTo("Básico");
        assertThat(warehouseLimit.get("detail").asText()).isNotBlank();

        // Sensors: 5
        long zone = createZone(owner, w1);
        for (int i = 0; i < 5; i++) {
            call(post("/api/v1/sensor-nodes").content(sensorJson(zone, i)), owner, 201);
        }
        JsonNode sensorLimit = body(call(post("/api/v1/sensor-nodes").content(sensorJson(zone, 5)), owner, 409));
        assertThat(sensorLimit.get("resource").asText()).isEqualTo("sensorNodes");
        assertThat(sensorLimit.get("used").asLong()).isEqualTo(5);
        assertThat(sensorLimit.get("limit").asInt()).isEqualTo(5);

        // Users: 3 (the owner included)
        createWorker(owner, "wa" + suffix, w1, 201);
        createWorker(owner, "wb" + suffix, w1, 201);
        JsonNode userLimit = body(createWorker(owner, "wc" + suffix, w1, 409));
        assertThat(userLimit.get("resource").asText()).isEqualTo("users");
        assertThat(userLimit.get("used").asLong()).isEqualTo(3);
        assertThat(userLimit.get("limit").asInt()).isEqualTo(3);

        JsonNode usage = body(call(get("/api/v1/subscriptions/current/usage"), owner, 200));
        assertThat(usage.get("planCode").asText()).isEqualTo("BASIC");
        assertThat(usage.get("subscriptionId").asLong()).isEqualTo(subscriptionId);
        Map<String, JsonNode> resources = new LinkedHashMap<>();
        usage.get("resources").forEach(node -> resources.put(node.get("resource").asText(), node));
        assertThat(resources.get("warehouses").get("used").asLong()).isEqualTo(1);
        assertThat(resources.get("sensorNodes").get("used").asLong()).isEqualTo(5);
        assertThat(resources.get("users").get("used").asLong()).isEqualTo(3);
        assertThat(resources.get("users").get("limit").asInt()).isEqualTo(3);

        // Upgrade unlocks the next resource; the downgrade is rejected while usage exceeds BASIC.
        call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan").content("{\"planCode\":\"PREMIUM\"}"), owner, 200);
        createWarehouse(owner, "W2");
        JsonNode downgrade = body(call(patch("/api/v1/subscriptions/" + subscriptionId + "/plan")
                .content("{\"planCode\":\"BASIC\"}"), owner, 409));
        assertThat(downgrade.get("code").asText()).isEqualTo("PLAN_USAGE_EXCEEDS_LIMITS");
        assertThat(downgrade.get("violations")).hasSize(1);
        assertThat(downgrade.get("violations").get(0).get("resource").asText()).isEqualTo("warehouses");
        // Nothing was deleted by the rejected change.
        assertThat(body(call(get("/api/v1/warehouses"), owner, 200))).hasSize(2);
    }

    @Test
    void ownerEditsProfileAndChangesPasswordSecurely() throws Exception {
        Session owner = signUp("prof" + suffix);

        JsonNode profile = body(call(get("/api/v1/profile"), owner, 200));
        assertThat(profile.get("username").asText()).isEqualTo("prof" + suffix);
        assertThat(profile.has("password")).isFalse();
        assertThat(profile.has("passwordHash")).isFalse();

        signUp("other" + suffix);
        call(patch("/api/v1/profile").content("{\"username\":\"other" + suffix + "\"}"), owner, 409);
        JsonNode updated = body(call(patch("/api/v1/profile").content("{\"fullName\":\"Ana Torres\",\"email\":\"ana"
                + suffix + "@nodesecure.test\",\"username\":\"ana" + suffix + "\"}"), owner, 200));
        assertThat(updated.get("fullName").asText()).isEqualTo("Ana Torres");
        assertThat(updated.get("username").asText()).isEqualTo("ana" + suffix);
        // The session keeps working after a username change.
        assertThat(body(call(get("/api/v1/authentication/me"), owner, 200)).get("username").asText())
                .isEqualTo("ana" + suffix);

        call(put("/api/v1/profile/password").content("{\"currentPassword\":\"wrong-pass\",\"newPassword\":\"NewSecret-456\"}"),
                owner, 400);
        JsonNode changed = body(call(put("/api/v1/profile/password")
                .content("{\"currentPassword\":\"Secret-123\",\"newPassword\":\"NewSecret-456\"}"), owner, 200));
        assertThat(changed.has("password")).isFalse();
        Session renewed = new Session(changed.get("token").asText(), owner.userId(), owner.organizationId());

        call(get("/api/v1/authentication/me"), owner, 401);       // old token revoked
        call(get("/api/v1/authentication/me"), renewed, 200);
        assertThat(signInStatus("ana" + suffix, "Secret-123")).isEqualTo(401);
        assertThat(signInStatus("ana" + suffix, "NewSecret-456")).isEqualTo(200);
        assertThat(mvc.perform(get("/api/v1/profile")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    // ------------------------------------------------------------------ helpers

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

    private int signInStatus(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andReturn().getResponse().getStatus();
    }

    private String warehouseJson(String name) {
        return "{\"name\":\"" + name + " " + suffix
                + "\",\"street\":\"Av. Test 123\",\"district\":\"Ate\",\"city\":\"Lima\",\"country\":\"PE\"}";
    }

    private long createWarehouse(Session owner, String name) throws Exception {
        return body(call(post("/api/v1/warehouses").content(warehouseJson(name)), owner, 201)).get("id").asLong();
    }

    private long createZone(Session owner, long warehouseId) throws Exception {
        return body(call(post("/api/v1/warehouse-zones").content("{\"warehouseId\":" + warehouseId
                + ",\"name\":\"Zona A\",\"code\":\"ZA\",\"riskLevel\":\"LOW\",\"openingShiftStart\":\"08:00\","
                + "\"openingShiftEnd\":\"18:00\"}"), owner, 201)).get("id").asLong();
    }

    private String sensorJson(long zoneId, int index) {
        String mac = String.format("02:%s:%s:%s:%s:%02X", suffix.substring(0, 2), suffix.substring(2, 4),
                suffix.substring(4, 6), "AB", index).toUpperCase();
        return "{\"zoneId\":" + zoneId + ",\"macAddress\":\"" + mac + "\",\"model\":\"ESP32\",\"sensorType\":\"MOTION\"}";
    }

    private MvcResult createWorker(Session owner, String username, long warehouseId, int status) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Worker " + username);
        body.put("email", username + "@nodesecure.test");
        body.put("username", username);
        body.put("password", "Secret-123");
        body.put("role", "ROLE_OPERATOR");
        body.put("warehouseIds", List.of(warehouseId));
        return call(post("/api/v1/users").content(json.writeValueAsString(body)), owner, status);
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
}
