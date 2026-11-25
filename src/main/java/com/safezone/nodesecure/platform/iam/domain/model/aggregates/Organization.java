package com.safezone.nodesecure.platform.iam.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * Tenant of the platform. Its id is the {@code company_id} stored in every business table.
 * An organization is only created by the public sign-up, together with its owner.
 */
public final class Organization extends AbstractDomainAggregateRoot {

    public enum Status { ACTIVE, SUSPENDED }

    private static final int MAX_NAME_LENGTH = 120;

    private final Long id;
    private String name;
    private Long ownerUserId;
    private Status status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Organization(Long id, String name, Long ownerUserId, Status status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = checkName(name);
        this.ownerUserId = ownerUserId;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Organization create(String name, Instant now) {
        return new Organization(null, name, null, Status.ACTIVE, now, now);
    }

    public static Organization restore(Long id, String name, Long ownerUserId, Status status,
                                       Instant createdAt, Instant updatedAt) {
        return new Organization(id, name, ownerUserId, status, createdAt, updatedAt);
    }

    public void assignOwner(Long userId, Instant now) {
        if (ownerUserId != null && !ownerUserId.equals(userId)) {
            throw new IllegalStateException("iam.error.organization.ownerAlreadyAssigned");
        }
        this.ownerUserId = Objects.requireNonNull(userId);
        this.updatedAt = now;
    }

    public void rename(String newName, Instant now) {
        this.name = checkName(newName);
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    private static String checkName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("iam.error.organization.name.notBlank");
        }
        String clean = name.trim();
        if (clean.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("iam.error.organization.name.size");
        }
        return clean;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
