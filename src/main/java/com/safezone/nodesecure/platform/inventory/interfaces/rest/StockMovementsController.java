package com.safezone.nodesecure.platform.inventory.interfaces.rest;

import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandService;
import com.safezone.nodesecure.platform.inventory.application.queries.GetAllStockMovementsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetStockMovementByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.StockMovementQueryService;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.RegisterStockMovementResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.StockMovementResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryValueObjectMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryProblemResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.RegisterStockMovementCommandMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.StockMovementCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.StockMovementResourceMapper;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@RestController
@RequestMapping(value = "/api/v1/stock-movements", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Stock Movements", description = "Immutable stock movement endpoints: entries, exits and reconciliations (US21, US22, US23, US24)")
public class StockMovementsController {
    private static final Logger log = LoggerFactory.getLogger(StockMovementsController.class);

    private final StockMovementCommandService stockMovementCommandService;
    private final StockMovementQueryService stockMovementQueryService;
    private final MessageSource messageSource;

    public StockMovementsController(StockMovementCommandService stockMovementCommandService,
                                    StockMovementQueryService stockMovementQueryService,
                                    MessageSource messageSource) {
        this.stockMovementCommandService = stockMovementCommandService;
        this.stockMovementQueryService = stockMovementQueryService;
        this.messageSource = messageSource;
    }


    @Operation(
            summary = "Register a stock movement",
            description = "ENTRY adds units to the stock (US21). EXIT removes units and fails if there is not enough stock (US22). "
                    + "RECONCILIATION replaces the stock with the counted quantity and requires a justification (US23). "
                    + "The product stock is updated in the same transaction and the movement can never be edited.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock movement registered",
                    content = @Content(schema = @Schema(implementation = StockMovementResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - insufficient stock for an exit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> registerStockMovement(@Valid @RequestBody RegisterStockMovementResource resource) {
        log.debug("POST /api/v1/stock-movements - productId={}, type={}", resource.productId(), resource.type());
        var result = stockMovementCommandService.handle(RegisterStockMovementCommandMapper.toCommandFromResource(resource));
        return StockMovementCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource);
    }

    @Operation(
            summary = "Get all stock movements",
            description = "Lists the movement history, newest first. Useful for the inventory dashboard: recent movements, "
                    + "latest reconciliations and product rotation (US24).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock movements retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StockMovementResource.class)))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<StockMovementResource>> getAllStockMovements(
            @Parameter(description = "Only movements of this product", example = "1")
            @RequestParam(name = "productId", required = false) Long productId,
            @Parameter(description = "Only movements of this warehouse", example = "1")
            @RequestParam(name = "warehouseId", required = false) Long warehouseId,
            @Parameter(description = "Only movements of this type", schema = @Schema(allowableValues = {"ENTRY", "EXIT", "RECONCILIATION"}))
            @RequestParam(name = "type", required = false) String type) {
        log.debug("GET /api/v1/stock-movements - productId={}, warehouseId={}, type={}", productId, warehouseId, type);
        var movementType = (type == null || type.isBlank())
                ? null
                : InventoryValueObjectMapper.toStockMovementTypeFromString(type);
        var movements = stockMovementQueryService.handle(new GetAllStockMovementsQuery(productId, warehouseId, movementType));
        return ResponseEntity.ok(movements.stream().map(StockMovementResourceMapper::toResourceFromEntity).toList());
    }

    @Operation(summary = "Get a stock movement by ID", description = "Gets one immutable stock movement.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock movement found",
                    content = @Content(schema = @Schema(implementation = StockMovementResource.class))),
            @ApiResponse(responseCode = "404", description = "Stock movement not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("{id}")
    public ResponseEntity<?> getStockMovementById(
            @Parameter(description = "Stock movement identifier", example = "1", required = true)
            @PathVariable Long id) {
        log.debug("GET /api/v1/stock-movements/{}", id);
        var movement = stockMovementQueryService.handle(new GetStockMovementByIdQuery(id));
        if (movement.isEmpty()) {
            return InventoryProblemResponseMapper.notFound(messageSource, "stock.error.notFoundById", id);
        }
        return ResponseEntity.ok(StockMovementResourceMapper.toResourceFromEntity(movement.get()));
    }
}
