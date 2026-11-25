package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Row of {@code user_permissions}: an exception to the role defaults (granted = true adds, false removes). */
@Embeddable
public class UserPermissionOverrideEmbeddable {

    @Column(name = "permission_code", nullable = false, length = 60)
    private String permissionCode;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    protected UserPermissionOverrideEmbeddable() {
    }

    public UserPermissionOverrideEmbeddable(String permissionCode, boolean granted, Long companyId) {
        this.permissionCode = permissionCode;
        this.granted = granted;
        this.companyId = companyId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public boolean isGranted() {
        return granted;
    }

    public Long getCompanyId() {
        return companyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserPermissionOverrideEmbeddable that)) return false;
        return granted == that.granted && Objects.equals(permissionCode, that.permissionCode)
                && Objects.equals(companyId, that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionCode, granted, companyId);
    }
}
