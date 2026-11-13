package com.safezone.nodesecure.platform.inventory.application.commandservices;

import com.safezone.nodesecure.platform.inventory.application.commands.RegisterStockMovementCommand;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.shared.application.result.Result;

public interface StockMovementCommandService {

    Result<StockMovement, StockMovementCommandFailure> handle(RegisterStockMovementCommand command);
}
