package com.safezone.nodesecure.platform.iam.domain.services;

import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission.*;

/**
 * Single source of truth for the permissions of each role.
 * Effective permissions = role defaults + granted overrides - revoked overrides.
 * Owner-only permissions are never effective for a worker, even if an override says so.
 */
public final class RolePermissions {

    private static final Set<Permission> SUPERVISOR_DEFAULTS = Collections.unmodifiableSet(EnumSet.of(
            DASHBOARD_VIEW, WAREHOUSES_VIEW,
            INVENTORY_VIEW, PRODUCTS_VIEW, PRODUCTS_CREATE, PRODUCTS_UPDATE,
            ENTRY_VIEW, ENTRY_CREATE, EXIT_VIEW, EXIT_CREATE, RECONCILE,
            TRACEABILITY_VIEW, IOT_DEVICES_VIEW, ALERTS_VIEW, ALERTS_MANAGE,
            REPORTS_VIEW, REPORTS_CREATE));

    private static final Set<Permission> OPERATOR_DEFAULTS = Collections.unmodifiableSet(EnumSet.of(
            DASHBOARD_VIEW, WAREHOUSES_VIEW,
            INVENTORY_VIEW, PRODUCTS_VIEW,
            ENTRY_VIEW, ENTRY_CREATE, EXIT_VIEW, EXIT_CREATE,
            TRACEABILITY_VIEW, ALERTS_VIEW));

    private RolePermissions() {
    }

    public static Set<Permission> defaultsFor(Role role) {
        return switch (role) {
            case ROLE_OWNER -> Collections.unmodifiableSet(EnumSet.allOf(Permission.class));
            case ROLE_SUPERVISOR -> SUPERVISOR_DEFAULTS;
            case ROLE_OPERATOR -> OPERATOR_DEFAULTS;
        };
    }

    public static Set<Permission> effective(Role role, Map<Permission, Boolean> overrides) {
        if (role.isOwner()) {
            return defaultsFor(role);
        }
        EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
        result.addAll(defaultsFor(role));
        if (overrides != null) {
            overrides.forEach((permission, granted) -> {
                if (Boolean.TRUE.equals(granted)) {
                    result.add(permission);
                } else {
                    result.remove(permission);
                }
            });
        }
        result.removeIf(Permission::ownerOnly);
        return Collections.unmodifiableSet(result);
    }
}
