package com.safezone.nodesecure.platform.warehouses.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.warehouses.application.commands.*;
import com.safezone.nodesecure.platform.warehouses.application.commandservices.WarehouseCommandService;
import com.safezone.nodesecure.platform.warehouses.application.queries.*;
import com.safezone.nodesecure.platform.warehouses.application.queryservices.WarehouseQueryService;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.CreateWarehouseResource;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.UpdateWarehouseResource;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform.WarehouseCommandMapper;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform.WarehouseHttpResponseMapper;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform.WarehouseResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Warehouses of the signed-in user's organization. Workers only see their assigned warehouses;
 * creating, editing and deleting warehouses is reserved to the owner (owner-only permissions).
 */
@RestController
@RequestMapping(value = "/api/v1/warehouses", produces = "application/json")
@Tag(name = "Warehouses", description = "Company warehouse infrastructure")
public class WarehousesController {

    private final WarehouseCommandService commandService;
    private final WarehouseQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public WarehousesController(WarehouseCommandService commandService, WarehouseQueryService queryService,
                                IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "List warehouses", description = "Owner: every warehouse of the organization. Workers: assigned warehouses only.")
    @PreAuthorize("hasAuthority('warehouses.view')")
    @GetMapping
    public ResponseEntity<List<?>> getAll() {
        List<?> resources = queryService.handle(new GetAllWarehousesQuery(iamFacade.requireCompanyId(),
                        iamFacade.warehouseScope())).stream()
                .map(WarehouseResourceMapper::toResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get warehouse by id")
    @PreAuthorize("hasAuthority('warehouses.view')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return queryService.handle(new GetWarehouseByIdQuery(id, iamFacade.requireCompanyId(), iamFacade.warehouseScope()))
                .<ResponseEntity<?>>map(warehouse -> ResponseEntity.ok(WarehouseResourceMapper.toResource(warehouse)))
                .orElseGet(() -> WarehouseHttpResponseMapper.notFound("warehouse", id, messages));
    }

    @Operation(summary = "Create warehouse")
    @PreAuthorize("hasAuthority('warehouses.create')")
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateWarehouseResource resource) {
        var command = WarehouseCommandMapper.toCreateCommand(resource, iamFacade.requireCompanyId());
        return WarehouseHttpResponseMapper.toWarehouseResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update warehouse")
    @PreAuthorize("hasAuthority('warehouses.update')")
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseResource resource) {
        var command = WarehouseCommandMapper.toUpdateCommand(id, resource, iamFacade.requireCompanyId());
        return WarehouseHttpResponseMapper.toWarehouseResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete warehouse")
    @PreAuthorize("hasAuthority('warehouses.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return WarehouseHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteWarehouseCommand(id, iamFacade.requireCompanyId())), messages);
    }
}
