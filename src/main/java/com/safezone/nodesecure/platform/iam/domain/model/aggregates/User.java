package com.safezone.nodesecure.platform.iam.domain.model.aggregates;

import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.UserStatus;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.services.RolePermissions;
import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * User of an organization (tenant). The organization is fixed at creation time and never changes.
 * Warehouses define the scope of the resources a worker can reach; permissions define the operations.
 */
public final class User extends AbstractDomainAggregateRoot {

    private static final int MAX_NAME_LENGTH = 120;

    private final Long id;
    private final Username username;
    private EmailAddress email;
    private String fullName;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private long tokenVersion;
    private final Long companyId;
    private Set<Long> warehouseIds;
    private Map<Permission, Boolean> permissionOverrides;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(Long id, Username username, EmailAddress email, String fullName, String passwordHash, Role role,
                 UserStatus status, long tokenVersion, Long companyId, Set<Long> warehouseIds,
                 Map<Permission, Boolean> permissionOverrides, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.fullName = checkFullName(fullName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.status = Objects.requireNonNull(status);
        this.tokenVersion = tokenVersion;
        this.companyId = companyId;
        this.warehouseIds = copyIds(warehouseIds);
        this.permissionOverrides = copyOverrides(permissionOverrides);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /** Owner of a brand-new organization (public sign-up). Owners see every warehouse of their organization. */
    public static User registerOwner(Username username, EmailAddress email, String fullName, String passwordHash,
                                     Long companyId, Instant now) {
        User user = new User(null, username, email, fullName, passwordHash, Role.ROLE_OWNER, UserStatus.ACTIVE, 0,
                requireCompany(companyId), Set.of(), Map.of(), now, now);
        user.registerEvent(new UserRegisteredEvent(username.value(), companyId, now));
        return user;
    }

    /** Worker created directly by the owner. The account is active immediately. */
    public static User createWorker(Username username, EmailAddress email, String fullName, String passwordHash,
                                    Role role, Long companyId, Set<Long> warehouseIds, Instant now) {
        checkWorkerRole(role);
        User user = new User(null, username, email, fullName, passwordHash, role, UserStatus.ACTIVE, 0,
                requireCompany(companyId), checkWarehouses(warehouseIds), Map.of(), now, now);
        user.registerEvent(new UserRegisteredEvent(username.value(), companyId, now));
        return user;
    }

    public static User restore(Long id, Username username, EmailAddress email, String fullName, String passwordHash,
                               Role role, UserStatus status, long tokenVersion, Long companyId,
                               Set<Long> warehouseIds, Map<Permission, Boolean> permissionOverrides,
                               Instant createdAt, Instant updatedAt) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user id");
        }
        return new User(id, username, email, fullName, passwordHash, role, status, tokenVersion, companyId,
                warehouseIds, permissionOverrides, createdAt, updatedAt);
    }

    public void updateProfile(EmailAddress newEmail, String newFullName, Instant now) {
        if (newEmail != null) {
            this.email = newEmail;
        }
        if (newFullName != null) {
            this.fullName = checkFullName(newFullName);
        }
        touch(now);
    }

    public void changeRole(Role newRole, Instant now) {
        requireWorker();
        checkWorkerRole(newRole);
        this.role = newRole;
        touch(now);
    }

    public void assignWarehouses(Set<Long> newWarehouseIds, Instant now) {
        requireWorker();
        this.warehouseIds = checkWarehouses(newWarehouseIds);
        touch(now);
    }

    /** Replaces the per-user exceptions to the role defaults. Owner-only permissions cannot be granted. */
    public void replacePermissionOverrides(Map<Permission, Boolean> overrides, Instant now) {
        requireWorker();
        Map<Permission, Boolean> copy = copyOverrides(overrides);
        for (Map.Entry<Permission, Boolean> entry : copy.entrySet()) {
            if (entry.getKey().ownerOnly() && Boolean.TRUE.equals(entry.getValue())) {
                throw new IllegalArgumentException("iam.error.permission.ownerOnly");
            }
        }
        this.permissionOverrides = copy;
        touch(now);
    }

    /** Suspends the account and invalidates every token issued before. */
    public void suspend(Instant now) {
        requireWorker();
        this.status = UserStatus.SUSPENDED;
        this.tokenVersion++;
        touch(now);
    }

    public void reactivate(Instant now) {
        requireWorker();
        this.status = UserStatus.ACTIVE;
        touch(now);
    }

    public boolean isOwner() {
        return role.isOwner();
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean belongsTo(Long organizationId) {
        return companyId != null && companyId.equals(organizationId);
    }

    public Set<Permission> effectivePermissions() {
        return RolePermissions.effective(role, permissionOverrides);
    }

    /** First assigned warehouse, kept for clients that still read a single warehouse id. */
    public Long primaryWarehouseId() {
        return warehouseIds.isEmpty() ? null : warehouseIds.iterator().next();
    }

    private void requireWorker() {
        if (role.isOwner()) {
            throw new IllegalStateException("iam.error.owner.immutable");
        }
    }

    private void touch(Instant now) {
        this.updatedAt = Objects.requireNonNull(now);
    }

    private static Long requireCompany(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("iam.error.organization.required");
        }
        return companyId;
    }

    private static void checkWorkerRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("iam.error.role.invalid");
        }
        if (role.isOwner()) {
            throw new IllegalArgumentException("iam.error.role.ownerNotAssignable");
        }
    }

    private static Set<Long> checkWarehouses(Set<Long> ids) {
        Set<Long> copy = copyIds(ids);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("iam.error.warehouses.required");
        }
        return copy;
    }

    private static Set<Long> copyIds(Set<Long> ids) {
        TreeSet<Long> copy = new TreeSet<>();
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(id -> {
                if (id <= 0) {
                    throw new IllegalArgumentException("warehouse.error.id.invalid");
                }
                copy.add(id);
            });
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Map<Permission, Boolean> copyOverrides(Map<Permission, Boolean> overrides) {
        EnumMap<Permission, Boolean> copy = new EnumMap<>(Permission.class);
        if (overrides != null) {
            overrides.forEach((permission, granted) -> {
                if (permission != null && granted != null) {
                    copy.put(permission, granted);
                }
            });
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String checkFullName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("iam.error.fullName.notBlank");
        }
        String cleanName = name.trim();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("iam.error.fullName.size");
        }
        return cleanName;
    }

    public Long getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Set<Long> getWarehouseIds() {
        return warehouseIds;
    }

    public Map<Permission, Boolean> getPermissionOverrides() {
        return permissionOverrides;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
