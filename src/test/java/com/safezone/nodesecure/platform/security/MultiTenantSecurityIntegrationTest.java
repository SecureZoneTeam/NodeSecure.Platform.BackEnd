package com.safezone.nodesecure.platform.security;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * End-to-end authorization scenarios (spec section 15) against a real MySQL 8 container:
 * owner A / owner B isolation, worker Pepito, request tampering and access revocation.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "iam.seed-demo-data=false"
})
@AutoConfigureMockMvc
class MultiTenantSecurityIntegrationTest {

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

    @BeforeEach
    void isolationGuard() throws Exception {
        try (var connection = dataSource.getConnection()) {
            MySqlTestContainer.assertIsolated(connection.getMetaData().getURL());
        }
        suffix = UUID.randomUUID().toString().substring(0, 6);
    }

    // ---------------------------------------------------------------- scenario fixtures

    /** Owner A with W1 and W2, owner B with W3 and W4, Pepito (A, W1) and María (B, W3). */
    private record World(Session ownerA, Session ownerB, long w1, long w2, long w3, long w4,
                         long pepitoId, Session pepito, long mariaId, long productA, long productB) {
    }

    private record Session(String token, long userId, long organizationId) {
    }

    private World world() throws Exception {
        Session ownerA = signUp("ownera" + suffix, "Organización A " + suffix);
        Session ownerB = signUp("ownerb" + suffix, "Organización B " + suffix);
        long w1 = createWarehouse(ownerA, "Warehouse 1");
        long w2 = createWarehouse(ownerA, "Warehouse 2");
        long w3 = createWarehouse(ownerB, "Warehouse 3");
        long w4 = createWarehouse(ownerB, "Warehouse 4");
        long pepitoId = createWorker(ownerA, "pepito" + suffix, "ROLE_OPERATOR", List.of(w1));
        long mariaId = createWorker(ownerB, "maria" + suffix, "ROLE_OPERATOR", List.of(w3));
        long productA = createProduct(ownerA, "SKU-A-" + suffix.toUpperCase());
        long productB = createProduct(ownerB, "SKU-B-" + suffix.toUpperCase());
        Session pepito = signIn("pepito" + suffix);
        return new World(ownerA, ownerB, w1, w2, w3, w4, pepitoId, pepito, mariaId, productA, productB);
    }

    // ---------------------------------------------------------------- scenario 1: owner A

    @Test
    void ownerSignUpCreatesAnIndependentOrganizationAndCanManageIt() throws Exception {
        World world = world();

        JsonNode me = body(call(get("/api/v1/authentication/me"), world.ownerA(), 200));
        assertThat(me.get("role").asText()).isEqualTo("ROLE_OWNER");
        assertThat(me.get("allWarehouses").asBoolean()).isTrue();
        assertThat(me.get("organizationName").asText()).startsWith("Organización A");
        assertThat(world.ownerA().organizationId()).isNotEqualTo(world.ownerB().organizationId());

        assertThat(ids(call(get("/api/v1/warehouses"), world.ownerA(), 200))).containsExactlyInAnyOrder(world.w1(), world.w2());
        JsonNode pepito = body(call(get("/api/v1/users/" + world.pepitoId()), world.ownerA(), 200));
        assertThat(pepito.get("companyId").asLong()).isEqualTo(world.ownerA().organizationId());
        assertThat(pepito.get("warehouseIds").get(0).asLong()).isEqualTo(world.w1());
        assertThat(pepito.get("status").asText()).isEqualTo("ACTIVE");
    }

    // ---------------------------------------------------------------- scenario 2: isolation A / B

    @Test
    void ownerBCannotSeeAnythingOfOrganizationA() throws Exception {
        World world = world();
        Session b = world.ownerB();
        registerMovement(world.ownerA(), world.productA(), world.w1(), "ENTRY", 5, 201);

        assertThat(ids(call(get("/api/v1/warehouses"), b, 200))).containsExactlyInAnyOrder(world.w3(), world.w4());
        call(get("/api/v1/warehouses/" + world.w1()), b, 404);
        assertThat(ids(call(get("/api/v1/users"), b, 200))).doesNotContain(world.pepitoId(), world.ownerA().userId());
        call(get("/api/v1/users/" + world.pepitoId()), b, 404);
        assertThat(ids(call(get("/api/v1/products"), b, 200))).containsExactly(world.productB());
        assertThat(ids(call(get("/api/v1/products?companyId=" + world.ownerA().organizationId()), b, 200)))
                .containsExactly(world.productB());
        call(get("/api/v1/products/" + world.productA()), b, 404);
        assertThat(body(call(get("/api/v1/stock-movements"), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/stock-movements?warehouseId=" + world.w1()), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/sensor-nodes"), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/security-alerts?warehouseId=" + world.w1()), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/subscriptions"), b, 200))).isEmpty();
        assertThat(body(call(get("/api/v1/organizations/current"), b, 200)).get("id").asLong())
                .isEqualTo(b.organizationId());
    }

    // ---------------------------------------------------------------- scenario 3: worker Pepito

    @Test
    void pepitoOperatesInventoryOfHisWarehouseButCannotAdministrate() throws Exception {
        World world = world();
        Session pepito = world.pepito();

        JsonNode me = body(call(get("/api/v1/authentication/me"), pepito, 200));
        assertThat(me.get("role").asText()).isEqualTo("ROLE_OPERATOR");
        assertThat(me.get("allWarehouses").asBoolean()).isFalse();

        assertThat(ids(call(get("/api/v1/warehouses"), pepito, 200))).containsExactly(world.w1());
        call(get("/api/v1/warehouses/" + world.w2()), pepito, 404);
        call(get("/api/v1/products"), pepito, 200);

        JsonNode entry = registerMovement(pepito, world.productA(), world.w1(), "ENTRY", 10, 201);
        assertThat(entry.get("performedBy").asText()).isEqualTo("pepito" + suffix);
        registerMovement(pepito, world.productA(), world.w1(), "EXIT", 3, 201);
        registerMovement(pepito, world.productA(), world.w2(), "ENTRY", 1, 404);
        assertThat(body(call(get("/api/v1/stock-movements"), pepito, 200))).hasSize(2);

        call(post("/api/v1/warehouses").content(warehouseJson("Hacked")), pepito, 403);
        call(put("/api/v1/warehouses/" + world.w1()).content(updateWarehouseJson("Hacked")), pepito, 403);
        call(delete("/api/v1/warehouses/" + world.w1()), pepito, 403);
        call(post("/api/v1/products").content(productJson("SKU-HACK-" + suffix.toUpperCase())), pepito, 403);
        registerMovement(pepito, world.productA(), world.w1(), "RECONCILIATION", 1, 403);
        call(get("/api/v1/sensor-nodes"), pepito, 403);
        call(post("/api/v1/sensor-nodes").content("{\"zoneId\":1,\"macAddress\":\"AA:BB:CC:DD:EE:FF\",\"model\":\"ESP32\",\"sensorType\":\"MOTION\"}"),
                pepito, 403);
        call(get("/api/v1/users"), pepito, 403);
        call(post("/api/v1/users").content(workerJson("intruder" + suffix, "ROLE_SUPERVISOR", List.of(world.w1()))), pepito, 403);
        call(get("/api/v1/subscriptions"), pepito, 403);
        call(get("/api/v1/organizations/current"), pepito, 403);
        call(get("/api/v1/permissions"), pepito, 403);
    }

    // ---------------------------------------------------------------- scenario 4: tampering

    @Test
    void tamperedRequestsAreRejectedByTheBackend() throws Exception {
        World world = world();

        // Sign-up cannot join an existing organization, even if the body carries a companyId.
        Map<String, Object> signUp = signUpBody("intruder" + suffix, "Intruder Org");
        signUp.put("companyId", world.ownerA().organizationId());
        signUp.put("role", "ROLE_OWNER");
        JsonNode intruder = body(mvc.perform(post("/api/v1/authentication/sign-up")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(signUp)))
                .andReturn(), 201);
        assertThat(intruder.get("organizationId").asLong()).isNotEqualTo(world.ownerA().organizationId());

        // A worker is always created in the organization of the owner, whatever the body says.
        Map<String, Object> worker = workerBody("juan" + suffix, "ROLE_OPERATOR", List.of(world.w1()));
        worker.put("companyId", world.ownerB().organizationId());
        JsonNode juan = body(call(post("/api/v1/users").content(json.writeValueAsString(worker)), world.ownerA(), 201));
        assertThat(juan.get("companyId").asLong()).isEqualTo(world.ownerA().organizationId());

        // Owner A cannot assign warehouses of B nor create another owner.
        call(post("/api/v1/users").content(workerJson("carlos" + suffix, "ROLE_OPERATOR", List.of(world.w3()))), world.ownerA(), 400);
        call(post("/api/v1/users").content(workerJson("boss" + suffix, "ROLE_OWNER", List.of(world.w1()))), world.ownerA(), 400);

        // Owner B cannot touch Pepito; Pepito cannot change his own permissions.
        call(patch("/api/v1/users/" + world.pepitoId()).content("{\"role\":\"ROLE_SUPERVISOR\"}"), world.ownerB(), 404);
        call(post("/api/v1/users/" + world.pepitoId() + "/suspend"), world.ownerB(), 404);
        call(put("/api/v1/users/" + world.pepitoId() + "/permissions").content("{\"overrides\":{\"warehouses.update\":true}}"),
                world.pepito(), 403);
        // Owner-only permissions can never be granted to a worker.
        call(put("/api/v1/users/" + world.pepitoId() + "/permissions").content("{\"overrides\":{\"users.create\":true}}"),
                world.ownerA(), 403);

        // Cross-tenant ids are rejected.
        registerMovement(world.ownerB(), world.productA(), world.w3(), "ENTRY", 1, 404);
        registerMovement(world.ownerB(), world.productB(), world.w1(), "ENTRY", 1, 404);
        call(put("/api/v1/products/" + world.productA()).content(productJson("SKU-X-" + suffix.toUpperCase())), world.ownerB(), 404);
        call(put("/api/v1/warehouses/" + world.w1()).content(updateWarehouseJson("Hacked")), world.ownerB(), 404);

        // Missing or forged tokens.
        mvc.perform(get("/api/v1/warehouses")).andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
        call(get("/api/v1/warehouses"), new Session(world.ownerA().token() + "x", 0, 0), 401);
    }

    // ---------------------------------------------------------------- scenario 5: revocation

    @Test
    void revokedAccessStopsWorkingImmediatelyWithTheSameToken() throws Exception {
        World world = world();
        Session pepito = world.pepito();
        registerMovement(world.ownerA(), world.productA(), world.w1(), "ENTRY", 20, 201);

        // Permission removed: the same token can no longer register exits.
        call(put("/api/v1/users/" + world.pepitoId() + "/permissions").content("{\"overrides\":{\"inventory.exit.create\":false}}"),
                world.ownerA(), 200);
        registerMovement(pepito, world.productA(), world.w1(), "EXIT", 1, 403);
        registerMovement(pepito, world.productA(), world.w1(), "ENTRY", 1, 201);

        // Warehouse assignment removed: W1 disappears for the same token.
        call(patch("/api/v1/users/" + world.pepitoId()).content("{\"warehouseIds\":[" + world.w2() + "]}"), world.ownerA(), 200);
        call(get("/api/v1/warehouses/" + world.w1()), pepito, 404);
        assertThat(ids(call(get("/api/v1/warehouses"), pepito, 200))).containsExactly(world.w2());

        // Suspension: the token stops working and sign-in is refused.
        call(post("/api/v1/users/" + world.pepitoId() + "/suspend"), world.ownerA(), 200);
        call(get("/api/v1/warehouses"), pepito, 401);
        signInExpecting("pepito" + suffix, 403);

        // Reactivation does not revive old tokens, but a new sign-in works.
        call(post("/api/v1/users/" + world.pepitoId() + "/reactivate"), world.ownerA(), 200);
        call(get("/api/v1/warehouses"), pepito, 401);
        call(get("/api/v1/warehouses"), signIn("pepito" + suffix), 200);

        // The owner cannot be suspended by himself.
        call(post("/api/v1/users/" + world.ownerA().userId() + "/suspend"), world.ownerA(), 403);
    }

    // ---------------------------------------------------------------- helpers

    private Session signUp(String username, String organization) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(signUpBody(username, organization)))).andReturn();
        JsonNode body = body(result, 201);
        return new Session(body.get("token").asText(), body.get("id").asLong(), body.get("organizationId").asLong());
    }

    private Map<String, Object> signUpBody(String username, String organization) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Owner " + username);
        body.put("email", username + "@nodesecure.test");
        body.put("username", username);
        body.put("password", "Secret-123");
        body.put("organizationName", organization);
        return body;
    }

    private Session signIn(String username) throws Exception {
        JsonNode body = body(signInExpecting(username, 200), 200);
        return new Session(body.get("token").asText(), body.get("id").asLong(), body.get("organizationId").asLong());
    }

    private MvcResult signInExpecting(String username, int status) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Secret-123\"}")).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        return result;
    }

    private long createWarehouse(Session owner, String name) throws Exception {
        return body(call(post("/api/v1/warehouses").content(warehouseJson(name)), owner, 201)).get("id").asLong();
    }

    private String warehouseJson(String name) {
        return "{\"name\":\"" + name + " " + suffix + "\",\"street\":\"Av. Test 123\",\"district\":\"Ate\","
                + "\"city\":\"Lima\",\"country\":\"PE\"}";
    }

    private String updateWarehouseJson(String name) {
        return "{\"name\":\"" + name + " " + suffix + "\",\"street\":\"Av. Test 123\",\"district\":\"Ate\","
                + "\"city\":\"Lima\",\"country\":\"PE\",\"status\":\"ACTIVE\"}";
    }

    private long createWorker(Session owner, String username, String role, List<Long> warehouses) throws Exception {
        return body(call(post("/api/v1/users").content(workerJson(username, role, warehouses)), owner, 201)).get("id").asLong();
    }

    private Map<String, Object> workerBody(String username, String role, List<Long> warehouses) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Worker " + username);
        body.put("email", username + "@nodesecure.test");
        body.put("username", username);
        body.put("password", "Secret-123");
        body.put("role", role);
        body.put("warehouseIds", warehouses);
        return body;
    }

    private String workerJson(String username, String role, List<Long> warehouses) throws Exception {
        return json.writeValueAsString(workerBody(username, role, warehouses));
    }

    private long createProduct(Session owner, String sku) throws Exception {
        return body(call(post("/api/v1/products").content(productJson(sku)), owner, 201)).get("id").asLong();
    }

    private String productJson(String sku) {
        return "{\"name\":\"Product " + sku + "\",\"sku\":\"" + sku + "\",\"category\":\"NETWORKING\",\"unit\":\"UNIT\","
                + "\"companyId\":999,\"createdBy\":\"someone-else\"}";
    }

    private JsonNode registerMovement(Session session, long productId, long warehouseId, String type, int quantity,
                                      int expectedStatus) throws Exception {
        String justification = "RECONCILIATION".equals(type) ? ",\"justification\":\"Physical count of the test\"" : "";
        MvcResult result = call(post("/api/v1/stock-movements").content("{\"productId\":" + productId
                + ",\"warehouseId\":" + warehouseId + ",\"type\":\"" + type + "\",\"quantity\":" + quantity
                + ",\"performedBy\":\"fperez\"" + justification + "}"), session, expectedStatus);
        return expectedStatus == 201 ? body(result) : null;
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
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode body(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(expectedStatus);
        return body(result);
    }

    private List<Long> ids(MvcResult result) throws Exception {
        List<Long> ids = new ArrayList<>();
        body(result).forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }
}
