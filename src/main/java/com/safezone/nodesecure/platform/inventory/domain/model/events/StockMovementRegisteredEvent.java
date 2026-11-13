package com.safezone.nodesecure.platform.inventory.domain.model.events;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.time.Instant;

public record StockMovementRegisteredEvent(Long productId, Long warehouseId, StockMovementType type,
                                           int quantity, int resultingStock, Instant occurredAt)
        implements DomainEvent {
}
