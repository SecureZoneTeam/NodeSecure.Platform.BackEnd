package com.safezone.nodesecure.platform.inventory.application.internal.queryservices;

import com.safezone.nodesecure.platform.inventory.application.queries.GetAllStockMovementsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetStockMovementByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.StockMovementQueryService;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.repositories.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StockMovementQueryServiceImpl implements StockMovementQueryService {
    private final StockMovementRepository repository;

    public StockMovementQueryServiceImpl(StockMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StockMovement> handle(GetAllStockMovementsQuery query) {
        return repository.findAll(query.productId(), query.warehouseId(), query.type());
    }

    @Override
    public Optional<StockMovement> handle(GetStockMovementByIdQuery query) {
        return repository.findById(query.id());
    }
}
