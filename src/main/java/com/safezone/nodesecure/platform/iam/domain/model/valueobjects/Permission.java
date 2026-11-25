package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalog of granular permissions. The code is the authority checked with
 * {@code @PreAuthorize("hasAuthority('...')")} and the value used by the frontend.
 * Owner-only permissions can never be granted to a worker.
 */
public enum Permission {
    DASHBOARD_VIEW(Codes.DASHBOARD_VIEW, false),

    WAREHOUSES_VIEW(Codes.WAREHOUSES_VIEW, false),
    WAREHOUSES_CREATE(Codes.WAREHOUSES_CREATE, true),
    WAREHOUSES_UPDATE(Codes.WAREHOUSES_UPDATE, true),
    WAREHOUSES_DELETE(Codes.WAREHOUSES_DELETE, true),

    INVENTORY_VIEW(Codes.INVENTORY_VIEW, false),
    PRODUCTS_VIEW(Codes.PRODUCTS_VIEW, false),
    PRODUCTS_CREATE(Codes.PRODUCTS_CREATE, false),
    PRODUCTS_UPDATE(Codes.PRODUCTS_UPDATE, false),
    ENTRY_VIEW(Codes.ENTRY_VIEW, false),
    ENTRY_CREATE(Codes.ENTRY_CREATE, false),
    EXIT_VIEW(Codes.EXIT_VIEW, false),
    EXIT_CREATE(Codes.EXIT_CREATE, false),
    RECONCILE(Codes.RECONCILE, false),

    TRACEABILITY_VIEW(Codes.TRACEABILITY_VIEW, false),

    IOT_DEVICES_VIEW(Codes.IOT_DEVICES_VIEW, false),
    IOT_DEVICES_MANAGE(Codes.IOT_DEVICES_MANAGE, true),

    ALERTS_VIEW(Codes.ALERTS_VIEW, false),
    ALERTS_MANAGE(Codes.ALERTS_MANAGE, false),

    REPORTS_VIEW(Codes.REPORTS_VIEW, false),
    REPORTS_CREATE(Codes.REPORTS_CREATE, false),

    USERS_VIEW(Codes.USERS_VIEW, true),
    USERS_CREATE(Codes.USERS_CREATE, true),
    USERS_UPDATE(Codes.USERS_UPDATE, true),
    USERS_SUSPEND(Codes.USERS_SUSPEND, true),

    SUBSCRIPTIONS_VIEW(Codes.SUBSCRIPTIONS_VIEW, true),
    SUBSCRIPTIONS_MANAGE(Codes.SUBSCRIPTIONS_MANAGE, true),

    SETTINGS_VIEW(Codes.SETTINGS_VIEW, true),
    SETTINGS_MANAGE(Codes.SETTINGS_MANAGE, true);

    private static final Map<String, Permission> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Permission::code, Function.identity()));

    private final String code;
    private final boolean ownerOnly;

    Permission(String code, boolean ownerOnly) {
        this.code = code;
        this.ownerOnly = ownerOnly;
    }

    public String code() {
        return code;
    }

    public boolean ownerOnly() {
        return ownerOnly;
    }

    public static Optional<Permission> fromCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(BY_CODE.get(code.trim()));
    }

    /** Constants usable inside annotations: {@code @PreAuthorize("hasAuthority('inventory.view')")}. */
    public static final class Codes {
        public static final String DASHBOARD_VIEW = "dashboard.view";
        public static final String WAREHOUSES_VIEW = "warehouses.view";
        public static final String WAREHOUSES_CREATE = "warehouses.create";
        public static final String WAREHOUSES_UPDATE = "warehouses.update";
        public static final String WAREHOUSES_DELETE = "warehouses.delete";
        public static final String INVENTORY_VIEW = "inventory.view";
        public static final String PRODUCTS_VIEW = "inventory.products.view";
        public static final String PRODUCTS_CREATE = "inventory.products.create";
        public static final String PRODUCTS_UPDATE = "inventory.products.update";
        public static final String ENTRY_VIEW = "inventory.entry.view";
        public static final String ENTRY_CREATE = "inventory.entry.create";
        public static final String EXIT_VIEW = "inventory.exit.view";
        public static final String EXIT_CREATE = "inventory.exit.create";
        public static final String RECONCILE = "inventory.reconcile";
        public static final String TRACEABILITY_VIEW = "traceability.view";
        public static final String IOT_DEVICES_VIEW = "iot.devices.view";
        public static final String IOT_DEVICES_MANAGE = "iot.devices.manage";
        public static final String ALERTS_VIEW = "alerts.view";
        public static final String ALERTS_MANAGE = "alerts.manage";
        public static final String REPORTS_VIEW = "reports.view";
        public static final String REPORTS_CREATE = "reports.create";
        public static final String USERS_VIEW = "users.view";
        public static final String USERS_CREATE = "users.create";
        public static final String USERS_UPDATE = "users.update";
        public static final String USERS_SUSPEND = "users.suspend";
        public static final String SUBSCRIPTIONS_VIEW = "subscriptions.view";
        public static final String SUBSCRIPTIONS_MANAGE = "subscriptions.manage";
        public static final String SETTINGS_VIEW = "settings.view";
        public static final String SETTINGS_MANAGE = "settings.manage";

        private Codes() {
        }
    }
}
