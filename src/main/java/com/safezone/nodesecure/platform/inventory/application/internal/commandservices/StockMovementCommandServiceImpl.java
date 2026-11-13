package com.safezone.nodesecure.platform.inventory.application.internal.commandservices;

import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandFailure;
import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandService;
import com.safezone.nodesecure.platform.inventory.application.commands.RegisterStockMovementCommand;
import com.safezone.nodesecure.platform.inventory.domain.exceptions.InsufficientStockException;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.repositories.ProductRepository;
import com.safezone.nodesecure.platform.inventory.domain.repositories.StockMovementRepository;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class StockMovementCommandServiceImpl implements StockMovementCommandService {
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public StockMovementCommandServiceImpl(ProductRepository productRepository,
                                           StockMovementRepository movementRepository,
                                           ApplicationEventPublisher eventPublisher,
                                           Clock clock) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<StockMovement, StockMovementCommandFailure> handle(RegisterStockMovementCommand command) {
        // The product row is locked so two simultaneous movements cannot overwrite each other's stock.
        var found = productRepository.findByIdForUpdate(command.productId());
        if (found.isEmpty())
            return Result.failure(new StockMovementCommandFailure.ProductNotFound(command.productId()));

        var product = found.get();
        var now = clock.instant();
        var previousStock = product.getCurrentStock();
        try {
            switch (command.type()) {
                case ENTRY -> product.receive(command.quantity(), now);
                case EXIT -> product.dispatch(command.quantity(), now);
                case RECONCILIATION -> product.reconcileTo(command.quantity(), now);
            }
        } catch (InsufficientStockException e) {
            return Result.failure(new StockMovementCommandFailure.InsufficientStock(e.getRequested(), e.getAvailable()));
        }

        var movement = StockMovement.register(product.getId(), command.warehouseId(), command.type(),
                command.quantity(), previousStock, product.getCurrentStock(), command.performedBy(),
                command.justification(), now);
        var events = movement.pullDomainEvents();

        productRepository.save(product);
        var saved = movementRepository.save(movement);
        events.forEach(eventPublisher::publishEvent);
        return Result.success(saved);
    }
}
