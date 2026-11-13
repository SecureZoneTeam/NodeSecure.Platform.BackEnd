package com.safezone.nodesecure.platform.inventory.application.queryservices;

import com.safezone.nodesecure.platform.inventory.application.queries.GetAllProductsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetProductByIdQuery;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;

import java.util.List;
import java.util.Optional;

public interface ProductQueryService {

    List<Product> handle(GetAllProductsQuery query);
    Optional<Product> handle(GetProductByIdQuery query);
}
