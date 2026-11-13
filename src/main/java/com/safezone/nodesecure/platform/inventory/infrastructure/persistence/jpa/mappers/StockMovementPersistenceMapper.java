package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities.StockMovementJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class StockMovementPersistenceMapper {
    public StockMovementJpaEntity toEntity(StockMovement movement) {
        return new StockMovementJpaEntity(movement.getId(), movement.getProductId(), movement.getWarehouseId(),
                movement.getType().name(), movement.getQuantity(), movement.getPreviousStock(),
                movement.getResultingStock(), movement.getPerformedBy(), movement.getOccurredAt(),
                movement.getJustification());
    }

    public StockMovement toDomain(StockMovementJpaEntity entity) {
        return StockMovement.rehydrate(entity.getId(), entity.getProductId(), entity.getWarehouseId(),
                StockMovementType.valueOf(entity.getType()), entity.getQuantity(), entity.getPreviousStock(),
                entity.getResultingStock(), entity.getPerformedBy(), entity.getOccurredAt(),
                entity.getJustification());
    }
}
