package com.safezone.nodesecure.platform.inventory.application.internal.commandservices;

import com.safezone.nodesecure.platform.inventory.application.commandservices.ProductCommandFailure;
import com.safezone.nodesecure.platform.inventory.application.commandservices.ProductCommandService;
import com.safezone.nodesecure.platform.inventory.application.commands.CreateProductCommand;
import com.safezone.nodesecure.platform.inventory.application.commands.UpdateProductCommand;
import com.safezone.nodesecure.platform.inventory.domain.exceptions.DuplicateSkuException;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.repositories.ProductRepository;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ProductCommandServiceImpl implements ProductCommandService {
    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ProductCommandServiceImpl(ProductRepository repository, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Product, ProductCommandFailure> handle(CreateProductCommand command) {
        if (repository.existsByCompanyIdAndSku(command.companyId(), command.sku()))
            return Result.failure(new ProductCommandFailure.DuplicateSku());
        var product = Product.create(command.companyId(), command.name(), command.sku(), command.category(),
                command.unit(), command.createdBy(), clock.instant());
        // Events are pulled before saving: the saved instance is rebuilt from the database and carries none.
        var events = product.pullDomainEvents();
        try {
            var saved = repository.save(product);
            events.forEach(eventPublisher::publishEvent);
            return Result.success(saved);
        } catch (DuplicateSkuException e) {
            return Result.failure(new ProductCommandFailure.DuplicateSku());
        }
    }

    @Override
    @Transactional
    public Result<Product, ProductCommandFailure> handle(UpdateProductCommand command) {
        var existing = repository.findById(command.id());
        if (existing.isEmpty())
            return Result.failure(new ProductCommandFailure.NotFound(command.id()));
        var product = existing.get();
        if (repository.existsByCompanyIdAndSkuAndIdNot(product.getCompanyId(), command.sku(), product.getId()))
            return Result.failure(new ProductCommandFailure.DuplicateSku());
        product.updateDetails(command.name(), command.sku(), command.category(), command.unit(), clock.instant());
        try {
            return Result.success(repository.save(product));
        } catch (DuplicateSkuException e) {
            return Result.failure(new ProductCommandFailure.DuplicateSku());
        }
    }
}
