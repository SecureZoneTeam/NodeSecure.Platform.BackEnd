package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/**
 * Role of a user inside its organization.
 * OWNER is assigned only by the backend when an organization is created (public sign-up).
 * SUPERVISOR and OPERATOR are workers created directly by the owner.
 */
public enum Role {
    ROLE_OWNER,
    ROLE_SUPERVISOR,
    ROLE_OPERATOR;

    /** Parses "ROLE_SUPERVISOR" or "SUPERVISOR" (case insensitive). */
    public static Role from(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("iam.error.role.invalid");
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("iam.error.role.invalid");
        }
    }

    /**
     * Fallback for rows whose new {@code role} column is still empty (migration script not applied).
     * Legacy values (ROLE_ADMINISTRATOR, ...) are not trusted: the least privileged role is used.
     */
    public static Role fromLegacyRoles(String legacyRoles) {
        if (legacyRoles != null) {
            for (String candidate : legacyRoles.split(",")) {
                try {
                    return from(candidate);
                } catch (IllegalArgumentException ignored) {
                    // not one of the current roles
                }
            }
        }
        return ROLE_OPERATOR;
    }

    public boolean isOwner() {
        return this == ROLE_OWNER;
    }
}
