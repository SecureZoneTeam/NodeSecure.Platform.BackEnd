package com.safezone.nodesecure.platform.iam.domain.model.aggregates;

import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class User extends AbstractDomainAggregateRoot {

    private static final int MAX_NAME_LENGTH = 120;

    private final Long id;
    private final Username username;
    private EmailAddress email;
    private String fullName;
    private final String passwordHash;
    private Set<Role> roles;
    private final Long companyId;
    private Long warehouseId;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(Long id, Username username, EmailAddress email, String fullName, String passwordHash,
                 Set<Role> roles, Long companyId, Long warehouseId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.fullName = checkFullName(fullName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.roles = checkRoles(roles);
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static User create(Username username, EmailAddress email, String fullName, String passwordHash,
                              Set<Role> roles, Long companyId, Long warehouseId, Instant now) {
        User user = new User(null, username, email, fullName, passwordHash, roles, companyId, warehouseId, now, now);
        user.registerEvent(new UserRegisteredEvent(username.value(), companyId, now));
        return user;
    }

    public static User restore(Long id, Username username, EmailAddress email, String fullName, String passwordHash,
                               Set<Role> roles, Long companyId, Long warehouseId, Instant createdAt, Instant updatedAt) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user id");
        }
        return new User(id, username, email, fullName, passwordHash, roles, companyId, warehouseId, createdAt, updatedAt);
    }

    public void update(EmailAddress newEmail, String newFullName, Set<Role> newRoles, Long newWarehouseId, Instant now) {
        if (newEmail != null) {
            this.email = newEmail;
        }
        if (newFullName != null) {
            this.fullName = checkFullName(newFullName);
        }
        if (newRoles != null) {
            this.roles = checkRoles(newRoles);
        }
        if (newWarehouseId != null) {
            this.warehouseId = newWarehouseId;
        }
        this.updatedAt = Objects.requireNonNull(now);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public List<String> getRoleNames() {
        return roles.stream().sorted().map(Role::name).toList();
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

    private static Set<Role> checkRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("iam.error.roles.notEmpty");
        }
        return EnumSet.copyOf(roles);
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

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
