package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Row of {@code user_warehouses}: a warehouse of the organization assigned to a worker. */
@Embeddable
public class UserWarehouseAssignmentEmbeddable {

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    protected UserWarehouseAssignmentEmbeddable() {
    }

    public UserWarehouseAssignmentEmbeddable(Long warehouseId, Long companyId) {
        this.warehouseId = warehouseId;
        this.companyId = companyId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserWarehouseAssignmentEmbeddable that)) return false;
        return Objects.equals(warehouseId, that.warehouseId) && Objects.equals(companyId, that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, companyId);
    }
}
