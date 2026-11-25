package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Table {@code users}. The legacy columns {@code roles} and {@code warehouse_id} are kept untouched
 * (not updatable): authorization now reads {@code role}, {@code status}, {@code user_warehouses}
 * and {@code user_permissions}.
 */
@Entity
@Table(name = "user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** Legacy column, written only on insert so existing values are never overwritten. */
    @Column(name = "roles", nullable = false, length = 200, updatable = false)
    private String legacyRoles;

    @Column(name = "role", length = 30)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "company_id")
    private Long companyId;

    /** Legacy column, written only on insert so existing values are never overwritten. */
    @Column(name = "warehouse_id", updatable = false)
    private Long legacyWarehouseId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_warehouses", joinColumns = @JoinColumn(name = "user_id"))
    private Set<UserWarehouseAssignmentEmbeddable> warehouses = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    private Set<UserPermissionOverrideEmbeddable> permissions = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(Long id, String username, String email, String fullName, String passwordHash,
                         String legacyRoles, String role, String status, long tokenVersion, Long companyId,
                         Long legacyWarehouseId, Set<UserWarehouseAssignmentEmbeddable> warehouses,
                         Set<UserPermissionOverrideEmbeddable> permissions, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.legacyRoles = legacyRoles;
        this.role = role;
        this.status = status;
        this.tokenVersion = tokenVersion;
        this.companyId = companyId;
        this.legacyWarehouseId = legacyWarehouseId;
        this.warehouses = new HashSet<>(warehouses);
        this.permissions = new HashSet<>(permissions);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getLegacyRoles() {
        return legacyRoles;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getLegacyWarehouseId() {
        return legacyWarehouseId;
    }

    public Set<UserWarehouseAssignmentEmbeddable> getWarehouses() {
        return warehouses;
    }

    public Set<UserPermissionOverrideEmbeddable> getPermissions() {
        return permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
