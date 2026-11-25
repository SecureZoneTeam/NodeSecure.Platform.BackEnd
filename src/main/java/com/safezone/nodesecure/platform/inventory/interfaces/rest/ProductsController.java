package com.safezone.nodesecure.platform.inventory.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.inventory.application.commandservices.ProductCommandService;
import com.safezone.nodesecure.platform.inventory.application.queries.GetAllProductsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetProductByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.ProductQueryService;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.CreateProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.ProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.UpdateProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryCommandMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryProblemResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.ProductCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.ProductResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Product catalog of the signed-in user's organization. The organization is always the one of the
 * session: products of other organizations answer 404 and the {@code companyId} parameter is ignored.
 */
@RestController
@RequestMapping(value = "/api/v1/products", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Product catalog endpoints (US20)")
public class ProductsController {

    private static final Logger log = LoggerFactory.getLogger(ProductsController.class);

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messageSource;

    public ProductsController(ProductCommandService productCommandService,
                              ProductQueryService productQueryService,
                              IamContextFacade iamFacade,
                              MessageSource messageSource) {
        this.productCommandService = productCommandService;
        this.productQueryService = productQueryService;
        this.iamFacade = iamFacade;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Create a product", description = "Adds a product to the catalog of the organization. The initial stock is always zero (US20).")
    @PreAuthorize("hasAuthority('inventory.products.create')")
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductResource resource) {
        log.debug("POST /api/v1/products - sku={}", resource.sku());
        var command = InventoryCommandMapper.toCreateProductCommand(resource, iamFacade.requireCompanyId(),
                iamFacade.currentUsername());
        return ProductCommandHttpResponseMapper.toResponseEntityFromResult(
                productCommandService.handle(command), messageSource, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all products", description = "Product catalog of the organization with the current stock of every product.")
    @PreAuthorize("hasAnyAuthority('inventory.view', 'inventory.products.view')")
    @GetMapping
    public ResponseEntity<List<ProductResource>> getAllProducts(
            @Parameter(description = "Ignored: the organization of the session is always used")
            @RequestParam(name = "companyId", required = false) Long ignoredCompanyId) {
        var products = productQueryService.handle(new GetAllProductsQuery(iamFacade.requireCompanyId()));
        return ResponseEntity.ok(products.stream().map(ProductResourceMapper::toResourceFromEntity).toList());
    }

    @Operation(summary = "Get a product by id")
    @PreAuthorize("hasAnyAuthority('inventory.view', 'inventory.products.view')")
    @GetMapping("{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return productQueryService.handle(new GetProductByIdQuery(id, iamFacade.requireCompanyId()))
                .<ResponseEntity<?>>map(product -> ResponseEntity.ok(ProductResourceMapper.toResourceFromEntity(product)))
                .orElseGet(() -> InventoryProblemResponseMapper.notFound(messageSource, "product.error.notFoundById", id));
    }

    @Operation(summary = "Update a product", description = "Updates name, SKU, category and unit. The stock cannot be edited here: use stock movements.")
    @PreAuthorize("hasAuthority('inventory.products.update')")
    @PutMapping("{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductResource resource) {
        log.debug("PUT /api/v1/products/{}", id);
        var command = InventoryCommandMapper.toUpdateProductCommand(id, resource, iamFacade.requireCompanyId());
        return ProductCommandHttpResponseMapper.toResponseEntityFromResult(
                productCommandService.handle(command), messageSource, HttpStatus.OK);
    }
}
