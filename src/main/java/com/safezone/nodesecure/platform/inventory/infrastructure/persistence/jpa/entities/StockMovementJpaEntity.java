package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "stock_movement", indexes = {
        @Index(name = "idx_stock_movement_product_id", columnList = "product_id"),
        @Index(name = "idx_stock_movement_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_stock_movement_occurred_at", columnList = "occurred_at")
})
public class StockMovementJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private String type;
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;
    @Column(name = "previous_stock", nullable = false, updatable = false)
    private int previousStock;
    @Column(name = "resulting_stock", nullable = false, updatable = false)
    private int resultingStock;
    @Column(name = "performed_by", nullable = false, updatable = false, length = 100)
    private String performedBy;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
    @Column(name = "justification", updatable = false, length = 300)
    private String justification;

    protected StockMovementJpaEntity() {
    }

    public StockMovementJpaEntity(Long id, Long productId, Long warehouseId, String type, int quantity,
                                  int previousStock, int resultingStock, String performedBy, Instant occurredAt,
                                  String justification) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.type = type;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.resultingStock = resultingStock;
        this.performedBy = performedBy;
        this.occurredAt = occurredAt;
        this.justification = justification;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPreviousStock() {
        return previousStock;
    }

    public int getResultingStock() {
        return resultingStock;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getJustification() {
        return justification;
    }
}
