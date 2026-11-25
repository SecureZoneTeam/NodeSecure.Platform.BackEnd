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
        return repository.findAll(query.companyId(), query.productId(), query.warehouseId(), query.type(),
                        query.warehouseScope()).stream()
                .filter(movement -> query.allowedTypes().contains(movement.getType()))
                .toList();
    }

    @Override
    public Optional<StockMovement> handle(GetStockMovementByIdQuery query) {
        return repository.findById(query.id())
                .filter(movement -> query.companyId().equals(movement.getCompanyId()))
                .filter(movement -> query.warehouseScope() == null
                        || query.warehouseScope().contains(movement.getWarehouseId()))
                .filter(movement -> query.allowedTypes().contains(movement.getType()));
    }
}
