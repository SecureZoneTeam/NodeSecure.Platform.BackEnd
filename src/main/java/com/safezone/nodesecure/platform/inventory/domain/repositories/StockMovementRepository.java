package com.safezone.nodesecure.platform.inventory.domain.repositories;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

import java.util.List;
import java.util.Optional;


public interface StockMovementRepository {
    StockMovement save(StockMovement movement);

    Optional<StockMovement> findById(Long id);

    List<StockMovement> findAll(Long productId, Long warehouseId, StockMovementType type);
}
