package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(name = "uk_product_company_id_sku", columnNames = {"company_id", "sku"}))
public class ProductJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "sku", nullable = false, length = 32)
    private String sku;
    @Column(name = "category", nullable = false, length = 30)
    private String category;
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
    @Column(name = "current_stock", nullable = false)
    private int currentStock;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(Long id, Long companyId, String name, String sku, String category, String unit,
                            int currentStock, Instant createdAt, Instant updatedAt, String createdBy) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.unit = unit;
        this.currentStock = currentStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
