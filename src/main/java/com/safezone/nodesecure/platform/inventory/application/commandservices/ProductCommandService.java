package com.safezone.nodesecure.platform.inventory.application.commandservices;

import com.safezone.nodesecure.platform.inventory.application.commands.CreateProductCommand;
import com.safezone.nodesecure.platform.inventory.application.commands.UpdateProductCommand;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.shared.application.result.Result;


public interface ProductCommandService {

    Result<Product, ProductCommandFailure> handle(CreateProductCommand command);
    Result<Product, ProductCommandFailure> handle(UpdateProductCommand command);
}
