package com.safezone.nodesecure.platform.shared.infrastructure.persistence.jpa.strategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import static io.github.encryptorcode.pluralize.Pluralize.pluralize;

public class SnakeCasePhysicalNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment environment) {
        return toSnakeCase(name);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment environment) {
        return toSnakeCase(name);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment environment) {
        return toSnakeCase(toPlural(name));
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment environment) {
        return toSnakeCase(name);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment environment) {
        return toSnakeCase(name);
    }

    private Identifier toSnakeCase(Identifier name) {
        if (name == null) {
            return null;
        }
        String snakeName = name.getText()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
        return Identifier.toIdentifier(snakeName);
    }

    private Identifier toPlural(Identifier name) {
        return Identifier.toIdentifier(pluralize(name.getText()));
    }
}
