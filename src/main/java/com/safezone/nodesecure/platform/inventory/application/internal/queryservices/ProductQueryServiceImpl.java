package com.safezone.nodesecure.platform.inventory.application.internal.queryservices;

import com.safezone.nodesecure.platform.inventory.application.queries.GetAllProductsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetProductByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.ProductQueryService;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {
    private final ProductRepository repository;

    public ProductQueryServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Product> handle(GetAllProductsQuery query) {
        return query.companyId() == null
                ? repository.findAll()
                : repository.findAllByCompanyId(query.companyId());
    }

    @Override
    public Optional<Product> handle(GetProductByIdQuery query) {
        return repository.findById(query.id());
    }
}
