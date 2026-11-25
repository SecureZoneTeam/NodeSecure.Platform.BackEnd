package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Authenticated principal rebuilt from the database on every request.
 * {@code allWarehouses} is true for the owner, who can reach every warehouse of the organization.
 */
public record SessionUser(Long id, String username, String fullName, Long companyId, Role role,
                          boolean allWarehouses, Set<Long> warehouseIds, Set<Permission> permissions) {

    public SessionUser {
        warehouseIds = Collections.unmodifiableSet(new TreeSet<>(warehouseIds == null ? Set.of() : warehouseIds));
        permissions = permissions == null || permissions.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(permissions));
    }

    public boolean hasRole(String roleName) {
        return role != null && role.name().equals(roleName);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String code) {
        return Permission.fromCode(code).map(permissions::contains).orElse(false);
    }

    /** Scope check only; callers must still verify that the warehouse belongs to {@link #companyId()}. */
    public boolean canAccessWarehouse(Long warehouseId) {
        return warehouseId != null && (allWarehouses || warehouseIds.contains(warehouseId));
    }

    /** {@code null} means "every warehouse of the organization". */
    public Set<Long> warehouseScope() {
        return allWarehouses ? null : warehouseIds;
    }
}
