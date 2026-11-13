package com.safezone.nodesecure.platform.inventory.domain.model.aggregates;

import com.safezone.nodesecure.platform.inventory.domain.model.events.StockMovementRegisteredEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;

public final class StockMovement extends AbstractDomainAggregateRoot {
    private static final int JUSTIFICATION_MIN_LENGTH = 10;
    private static final int JUSTIFICATION_MAX_LENGTH = 300;
    private static final int PERFORMED_BY_MAX_LENGTH = 100;

    private final Long id;
    private final Long productId;
    private final Long warehouseId;
    private final StockMovementType type;
    private final int quantity;
    private final int previousStock;
    private final int resultingStock;
    private final String performedBy;
    private final Instant occurredAt;
    private final String justification;

    private StockMovement(Long id, Long productId, Long warehouseId, StockMovementType type, int quantity,
                          int previousStock, int resultingStock, String performedBy, Instant occurredAt,
                          String justification) {
        if (productId == null || productId <= 0) throw new IllegalArgumentException("stock.error.product.invalid");
        if (warehouseId == null || warehouseId <= 0) throw new IllegalArgumentException("stock.error.warehouse.invalid");
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.type = Objects.requireNonNull(type);
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.resultingStock = resultingStock;
        this.performedBy = normalizePerformedBy(performedBy);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.justification = justification;
    }

    public static StockMovement register(Long productId, Long warehouseId, StockMovementType type, int quantity,
                                         int previousStock, int resultingStock, String performedBy,
                                         String justification, Instant now) {
        if (type == null) throw new IllegalArgumentException("stock.error.type.required");
        if (previousStock < 0 || resultingStock < 0) throw new IllegalArgumentException("stock.error.countedStock.invalid");
        String normalizedJustification = null;
        if (type == StockMovementType.RECONCILIATION) {
            if (quantity < 0) throw new IllegalArgumentException("stock.error.countedStock.invalid");
            normalizedJustification = normalizeJustification(justification);
        } else if (quantity <= 0) {
            throw new IllegalArgumentException("stock.error.quantity.invalid");
        }
        var movement = new StockMovement(null, productId, warehouseId, type, quantity, previousStock,
                resultingStock, performedBy, now, normalizedJustification);
        movement.registerEvent(new StockMovementRegisteredEvent(productId, warehouseId, type, quantity,
                resultingStock, now));
        return movement;
    }

    public static StockMovement rehydrate(Long id, Long productId, Long warehouseId, StockMovementType type,
                                          int quantity, int previousStock, int resultingStock, String performedBy,
                                          Instant occurredAt, String justification) {
        if (id == null || id <= 0) throw new IllegalArgumentException("stock.error.id.invalid");
        return new StockMovement(id, productId, warehouseId, type, quantity, previousStock, resultingStock,
                performedBy, occurredAt, justification);
    }

    private static String normalizeJustification(String justification) {
        if (justification == null) throw new IllegalArgumentException("stock.error.justification.invalid");
        var trimmed = justification.trim();
        if (trimmed.length() < JUSTIFICATION_MIN_LENGTH || trimmed.length() > JUSTIFICATION_MAX_LENGTH) {
            throw new IllegalArgumentException("stock.error.justification.invalid");
        }
        return trimmed;
    }

    private static String normalizePerformedBy(String performedBy) {
        if (performedBy == null || performedBy.isBlank()) throw new IllegalArgumentException("stock.error.performedBy.notBlank");
        var trimmed = performedBy.trim();
        if (trimmed.length() > PERFORMED_BY_MAX_LENGTH) throw new IllegalArgumentException("stock.error.performedBy.size");
        return trimmed;
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

    public StockMovementType getType() {
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
