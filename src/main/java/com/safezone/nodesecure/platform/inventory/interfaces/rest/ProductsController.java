package com.safezone.nodesecure.platform.inventory.interfaces.rest;

import com.safezone.nodesecure.platform.inventory.application.commandservices.ProductCommandService;
import com.safezone.nodesecure.platform.inventory.application.queries.GetAllProductsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetProductByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.ProductQueryService;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.CreateProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.ProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.UpdateProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.CreateProductCommandMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryProblemResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.ProductCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.ProductResourceMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.UpdateProductCommandMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@RequestMapping(value = "/api/v1/products", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Product catalog endpoints (US20)")
public class ProductsController {
    private static final Logger log = LoggerFactory.getLogger(ProductsController.class);

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;
    private final MessageSource messageSource;

    public ProductsController(ProductCommandService productCommandService,
                              ProductQueryService productQueryService,
                              MessageSource messageSource) {
        this.productCommandService = productCommandService;
        this.productQueryService = productQueryService;
        this.messageSource = messageSource;
    }


    @Operation(
            summary = "Create a product",
            description = "Adds a product (name, SKU, category) to the catalog of a company. The initial stock is always zero (US20).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(schema = @Schema(implementation = ProductResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - the SKU already exists for the company",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductResource resource) {
        log.debug("POST /api/v1/products - sku={}", resource.sku());
        var result = productCommandService.handle(CreateProductCommandMapper.toCommandFromResource(resource));
        return ProductCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.CREATED);
    }


    @Operation(
            summary = "Get all products",
            description = "Lists the product catalog with the current stock of every product. Can be filtered by company.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductResource.class)))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProductResource>> getAllProducts(
            @Parameter(description = "Only products of this company", example = "1")
            @RequestParam(name = "companyId", required = false) Long companyId) {
        log.debug("GET /api/v1/products - companyId={}", companyId);
        var products = productQueryService.handle(new GetAllProductsQuery(companyId));
        return ResponseEntity.ok(products.stream().map(ProductResourceMapper::toResourceFromEntity).toList());
    }


    @Operation(summary = "Get a product by ID", description = "Gets a product with its current stock.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResource.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("{id}")
    public ResponseEntity<?> getProductById(
            @Parameter(description = "Product identifier", example = "1", required = true)
            @PathVariable Long id) {
        log.debug("GET /api/v1/products/{}", id);
        var product = productQueryService.handle(new GetProductByIdQuery(id));
        if (product.isEmpty()) {
            return InventoryProblemResponseMapper.notFound(messageSource, "product.error.notFoundById", id);
        }
        return ResponseEntity.ok(ProductResourceMapper.toResourceFromEntity(product.get()));
    }


    @Operation(
            summary = "Update a product",
            description = "Updates name, SKU, category and unit. The stock cannot be edited here: use stock movements.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated",
                    content = @Content(schema = @Schema(implementation = ProductResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - the SKU already exists for the company",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("{id}")
    public ResponseEntity<?> updateProduct(
            @Parameter(description = "Product identifier", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductResource resource) {
        log.debug("PUT /api/v1/products/{}", id);
        var result = productCommandService.handle(UpdateProductCommandMapper.toCommandFromResource(id, resource));
        return ProductCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.OK);
    }
}
