package com.safezone.nodesecure.platform.inventory.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandService;
import com.safezone.nodesecure.platform.inventory.application.queries.GetAllStockMovementsQuery;
import com.safezone.nodesecure.platform.inventory.application.queries.GetStockMovementByIdQuery;
import com.safezone.nodesecure.platform.inventory.application.queryservices.StockMovementQueryService;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.RegisterStockMovementResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.StockMovementResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryCommandMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryProblemResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.InventoryValueObjectMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.StockMovementCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.transform.StockMovementResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Immutable stock movements. Each movement type has its own permission (entry, exit, reconciliation)
 * and the warehouse must belong to the organization and be assigned to the user.
 */
@RestController
@RequestMapping(value = "/api/v1/stock-movements", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Stock Movements", description = "Immutable stock movement endpoints: entries, exits and reconciliations (US21, US22, US23, US24)")
public class StockMovementsController {

    private static final Logger log = LoggerFactory.getLogger(StockMovementsController.class);

    private final StockMovementCommandService stockMovementCommandService;
    private final StockMovementQueryService stockMovementQueryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messageSource;

    public StockMovementsController(StockMovementCommandService stockMovementCommandService,
                                    StockMovementQueryService stockMovementQueryService,
                                    IamContextFacade iamFacade,
                                    MessageSource messageSource) {
        this.stockMovementCommandService = stockMovementCommandService;
        this.stockMovementQueryService = stockMovementQueryService;
        this.iamFacade = iamFacade;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Register a stock movement",
            description = "ENTRY requires inventory.entry.create, EXIT inventory.exit.create and RECONCILIATION "
                    + "inventory.reconcile. The warehouse must be assigned to the user. The movement can never be edited.")
    @PostMapping
    public ResponseEntity<?> registerStockMovement(@Valid @RequestBody RegisterStockMovementResource resource) {
        log.debug("POST /api/v1/stock-movements - productId={}, type={}", resource.productId(), resource.type());
        var type = InventoryValueObjectMapper.toStockMovementTypeFromString(resource.type());
        iamFacade.requirePermission(createPermissionOf(type));
        if (!iamFacade.canAccessWarehouse(resource.warehouseId())) {
            return InventoryProblemResponseMapper.notFound(messageSource, "stock.error.warehouseNotFound",
                    resource.warehouseId());
        }
        var command = InventoryCommandMapper.toRegisterStockMovementCommand(resource, iamFacade.requireCompanyId(),
                iamFacade.currentUsername());
        return StockMovementCommandHttpResponseMapper.toResponseEntityFromResult(
                stockMovementCommandService.handle(command), messageSource);
    }

    @Operation(summary = "Get all stock movements",
            description = "Movement history of the organization limited to the user's warehouses, newest first. "
                    + "Only the movement types the user can view are returned.")
    @PreAuthorize("hasAnyAuthority('inventory.view', 'inventory.entry.view', 'inventory.exit.view', 'traceability.view')")
    @GetMapping
    public ResponseEntity<List<StockMovementResource>> getAllStockMovements(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "warehouseId", required = false) Long warehouseId,
            @RequestParam(name = "type", required = false) String type) {
        var movementType = (type == null || type.isBlank())
                ? null
                : InventoryValueObjectMapper.toStockMovementTypeFromString(type);
        Set<StockMovementType> allowedTypes = viewableTypes();
        if (movementType != null && !allowedTypes.contains(movementType)) {
            throw new AccessDeniedException("Movement type not viewable: " + movementType);
        }
        var movements = stockMovementQueryService.handle(new GetAllStockMovementsQuery(iamFacade.requireCompanyId(),
                productId, warehouseId, movementType, iamFacade.warehouseScope(), allowedTypes));
        return ResponseEntity.ok(movements.stream().map(StockMovementResourceMapper::toResourceFromEntity).toList());
    }

    @Operation(summary = "Get a stock movement by id")
    @PreAuthorize("hasAnyAuthority('inventory.view', 'inventory.entry.view', 'inventory.exit.view', 'traceability.view')")
    @GetMapping("{id}")
    public ResponseEntity<?> getStockMovementById(@PathVariable Long id) {
        return stockMovementQueryService.handle(new GetStockMovementByIdQuery(id, iamFacade.requireCompanyId(),
                        iamFacade.warehouseScope(), viewableTypes()))
                .<ResponseEntity<?>>map(movement -> ResponseEntity.ok(StockMovementResourceMapper.toResourceFromEntity(movement)))
                .orElseGet(() -> InventoryProblemResponseMapper.notFound(messageSource, "stock.error.notFoundById", id));
    }

    private static String createPermissionOf(StockMovementType type) {
        return switch (type) {
            case ENTRY -> "inventory.entry.create";
            case EXIT -> "inventory.exit.create";
            case RECONCILIATION -> "inventory.reconcile";
        };
    }

    /** Traceability gives a read-only view of every movement type. */
    private Set<StockMovementType> viewableTypes() {
        boolean traceability = iamFacade.hasPermission("traceability.view");
        Set<StockMovementType> types = EnumSet.noneOf(StockMovementType.class);
        if (traceability || iamFacade.hasPermission("inventory.entry.view")) types.add(StockMovementType.ENTRY);
        if (traceability || iamFacade.hasPermission("inventory.exit.view")) types.add(StockMovementType.EXIT);
        if (traceability || iamFacade.hasPermission("inventory.view")) types.add(StockMovementType.RECONCILIATION);
        return types;
    }
}
