package com.safezone.nodesecure.platform.inventory.domain.repositories;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

import java.util.List;
import java.util.Optional;


public interface StockMovementRepository {
    StockMovement save(StockMovement movement);

    Optional<StockMovement> findById(Long id);

    /** Movements of one organization; {@code warehouseScope} null = every warehouse, empty = none. */
    List<StockMovement> findAll(Long companyId, Long productId, Long warehouseId, StockMovementType type,
                                java.util.Collection<Long> warehouseScope);
}
