package com.safezone.nodesecure.platform.iam.domain.model.aggregates;

import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.UserStatus;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private User newWorker() {
        return User.createWorker(new Username("pepito"), new EmailAddress("pepito@andina.pe"),
                "Pepito Pérez", "hash", Role.ROLE_OPERATOR, 1L, Set.of(1L), NOW);
    }

    private User restoredWorker() {
        return User.restore(2L, new Username("pepito"), new EmailAddress("pepito@andina.pe"), "Pepito Pérez",
                "hash", Role.ROLE_OPERATOR, UserStatus.ACTIVE, 0, 1L, Set.of(1L), Map.of(), NOW, NOW);
    }

    @Test
    void createRegistersEventOnlyOnce() {
        User user = newWorker();

        assertNull(user.getId());
        List<DomainEvent> events = user.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(UserRegisteredEvent.class, events.get(0));
        assertTrue(user.pullDomainEvents().isEmpty());
    }

    @Test
    void ownerHasEveryPermissionAndEveryWarehouse() {
        User owner = User.registerOwner(new Username("ownera"), new EmailAddress("owner@a.pe"), "Owner A", "hash", 1L, NOW);

        assertTrue(owner.isOwner());
        assertEquals(Set.of(Permission.values()), owner.effectivePermissions());
        assertTrue(owner.getWarehouseIds().isEmpty());
    }

    @Test
    void workerCannotBeCreatedAsOwnerOrWithoutWarehouse() {
        assertEquals("iam.error.role.ownerNotAssignable", assertThrows(IllegalArgumentException.class, () ->
                User.createWorker(new Username("x-user"), new EmailAddress("x@a.pe"), "X", "hash",
                        Role.ROLE_OWNER, 1L, Set.of(1L), NOW)).getMessage());
        assertEquals("iam.error.warehouses.required", assertThrows(IllegalArgumentException.class, () ->
                User.createWorker(new Username("x-user"), new EmailAddress("x@a.pe"), "X", "hash",
                        Role.ROLE_OPERATOR, 1L, Set.of(), NOW)).getMessage());
    }

    @Test
    void restoreDoesNotRegisterEvents() {
        User user = restoredWorker();

        assertEquals(2L, user.getId());
        assertTrue(user.pullDomainEvents().isEmpty());
    }

    @Test
    void operatorDefaultsDoNotIncludeAdministrativePermissions() {
        Set<Permission> permissions = newWorker().effectivePermissions();

        assertTrue(permissions.contains(Permission.ENTRY_CREATE));
        assertTrue(permissions.contains(Permission.EXIT_CREATE));
        assertFalse(permissions.contains(Permission.WAREHOUSES_UPDATE));
        assertFalse(permissions.contains(Permission.PRODUCTS_CREATE));
        assertFalse(permissions.contains(Permission.IOT_DEVICES_MANAGE));
        assertFalse(permissions.contains(Permission.USERS_VIEW));
        assertFalse(permissions.contains(Permission.SUBSCRIPTIONS_VIEW));
    }

    @Test
    void overridesGrantAndRevokePermissions() {
        User user = restoredWorker();

        user.replacePermissionOverrides(Map.of(Permission.PRODUCTS_CREATE, true, Permission.EXIT_CREATE, false), NOW);

        assertTrue(user.effectivePermissions().contains(Permission.PRODUCTS_CREATE));
        assertFalse(user.effectivePermissions().contains(Permission.EXIT_CREATE));
    }

    @Test
    void ownerOnlyPermissionsCannotBeGranted() {
        User user = restoredWorker();

        assertEquals("iam.error.permission.ownerOnly", assertThrows(IllegalArgumentException.class, () ->
                user.replacePermissionOverrides(Map.of(Permission.WAREHOUSES_UPDATE, true), NOW)).getMessage());
    }

    @Test
    void suspendInvalidatesTokensAndReactivateRestoresAccess() {
        User user = restoredWorker();

        user.suspend(NOW);
        assertFalse(user.isActive());
        assertEquals(1, user.getTokenVersion());

        user.reactivate(NOW);
        assertTrue(user.isActive());
        assertEquals(1, user.getTokenVersion());
    }

    @Test
    void ownerCannotBeSuspendedOrChanged() {
        User owner = User.restore(1L, new Username("ownera"), new EmailAddress("owner@a.pe"), "Owner A", "hash",
                Role.ROLE_OWNER, UserStatus.ACTIVE, 0, 1L, Set.of(), Map.of(), NOW, NOW);

        assertThrows(IllegalStateException.class, () -> owner.suspend(NOW));
        assertThrows(IllegalStateException.class, () -> owner.changeRole(Role.ROLE_OPERATOR, NOW));
        assertThrows(IllegalStateException.class, () -> owner.assignWarehouses(Set.of(1L), NOW));
    }

    @Test
    void updateProfileChangesOnlyGivenFields() {
        User user = restoredWorker();
        Instant later = NOW.plusSeconds(60);

        user.updateProfile(null, "Pepito Pérez Ruiz", later);

        assertEquals("pepito@andina.pe", user.getEmail().value());
        assertEquals("Pepito Pérez Ruiz", user.getFullName());
        assertEquals(later, user.getUpdatedAt());
    }

    @Test
    void legacyRolesFallBackToOperator() {
        assertEquals(Role.ROLE_OPERATOR, Role.fromLegacyRoles("ROLE_ADMINISTRATOR"));
        assertEquals(Role.ROLE_SUPERVISOR, Role.fromLegacyRoles("ROLE_SUPERVISOR"));
        assertEquals(Role.ROLE_OPERATOR, Role.fromLegacyRoles(null));
    }
}
