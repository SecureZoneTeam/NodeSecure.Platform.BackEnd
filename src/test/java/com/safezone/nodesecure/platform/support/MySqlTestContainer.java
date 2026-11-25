package com.safezone.nodesecure.platform.support;

import org.testcontainers.containers.MySQLContainer;

import java.util.Locale;

/**
 * Single isolated MySQL 8 container shared by every integration test of the run.
 * It is the only database the tests ever touch: production (FreeDB) is never used.
 */
public final class MySqlTestContainer {

    /** Same major/minor version as FreeDB (8.0.x). */
    private static final MySQLContainer<?> CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("node_secure_it")
            .withUsername("root")
            .withPassword("it-root-password")
            .withUrlParam("allowPublicKeyRetrieval", "true")
            .withUrlParam("useSSL", "false");

    static {
        CONTAINER.start();
    }

    private MySqlTestContainer() {
    }

    public static MySQLContainer<?> get() {
        return CONTAINER;
    }

    /** JDBC url of another schema inside the same container. */
    public static String jdbcUrlFor(String schema) {
        return CONTAINER.getJdbcUrl().replace("/" + CONTAINER.getDatabaseName(), "/" + schema);
    }

    /** Guard: aborts if a test is about to run against anything that is not a local/container database. */
    public static void assertIsolated(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase(Locale.ROOT);
        if (url.contains("freedb") || !(url.startsWith("jdbc:mysql://localhost") || url.startsWith("jdbc:mysql://127.0.0.1")
                || url.startsWith("jdbc:mysql://" + CONTAINER.getHost().toLowerCase(Locale.ROOT)))) {
            throw new IllegalStateException("Integration tests must only run against the isolated test container: " + jdbcUrl);
        }
    }
}
