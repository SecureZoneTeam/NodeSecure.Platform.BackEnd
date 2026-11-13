package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.StockMovementResource;

public class StockMovementResourceMapper {
    public static StockMovementResource toResourceFromEntity(StockMovement entity) {
        return new StockMovementResource(
                entity.getId(),
                entity.getProductId(),
                entity.getWarehouseId(),
                entity.getType().name(),
                entity.getQuantity(),
                entity.getPreviousStock(),
                entity.getResultingStock(),
                entity.getPerformedBy(),
                entity.getOccurredAt(),
                entity.getJustification());
    }
}
