package com.safezone.nodesecure.platform.inventory.application.queryservices;

import com.safezone.nodesecure.platform.inventory.application.queries.GetAllStockMovementsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetStockMovementByIdQuery;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;

import java.util.List;
import java.util.Optional;

public interface StockMovementQueryService {

    List<StockMovement> handle(GetAllStockMovementsQuery query);
    Optional<StockMovement> handle(GetStockMovementByIdQuery query);
}
