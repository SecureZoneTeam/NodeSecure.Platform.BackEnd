package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

public enum Role {
    ROLE_ADMINISTRATOR,
    ROLE_SECURITY_MANAGER,
    ROLE_WAREHOUSE_KEEPER,
    ROLE_AUDITOR;

    public static Role from(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("iam.error.role.invalid");
        }
        try {
            return Role.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("iam.error.role.invalid");
        }
    }
}
