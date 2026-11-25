package com.safezone.nodesecure.platform.warehouses.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.warehouses.application.commands.*;
import com.safezone.nodesecure.platform.warehouses.application.commandservices.WarehouseCommandService;
import com.safezone.nodesecure.platform.warehouses.application.queries.*;
import com.safezone.nodesecure.platform.warehouses.application.queryservices.WarehouseQueryService;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.*;
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

/** Zones are part of the warehouse structure: reading follows the warehouse scope, changes are owner-only. */
@RestController
@RequestMapping(value = "/api/v1/warehouse-zones", produces = "application/json")
@Tag(name = "Warehouse Zones", description = "Operating zones and authorized shifts")
public class WarehouseZonesController {

    private final WarehouseCommandService commandService;
    private final WarehouseQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public WarehouseZonesController(WarehouseCommandService commandService, WarehouseQueryService queryService,
                                    IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "List warehouse zones")
    @PreAuthorize("hasAuthority('warehouses.view')")
    @GetMapping
    public ResponseEntity<List<WarehouseZoneResource>> getAll() {
        List<WarehouseZoneResource> resources = queryService.handle(
                        new GetAllWarehouseZonesQuery(iamFacade.requireCompanyId(), iamFacade.warehouseScope()))
                .stream().map(WarehouseResourceMapper::toZoneResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get zone by id")
    @PreAuthorize("hasAuthority('warehouses.view')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return queryService.handle(new GetWarehouseZoneByIdQuery(id, iamFacade.requireCompanyId(), iamFacade.warehouseScope()))
                .<ResponseEntity<?>>map(zone -> ResponseEntity.ok(WarehouseResourceMapper.toZoneResource(zone)))
                .orElseGet(() -> WarehouseHttpResponseMapper.notFound("warehouseZone", id, messages));
    }

    @Operation(summary = "Create zone")
    @PreAuthorize("hasAuthority('warehouses.update')")
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateWarehouseZoneResource resource) {
        var command = WarehouseCommandMapper.toCreateZoneCommand(resource, iamFacade.requireCompanyId());
        return WarehouseHttpResponseMapper.toZoneResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update zone")
    @PreAuthorize("hasAuthority('warehouses.update')")
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseZoneResource resource) {
        var command = WarehouseCommandMapper.toUpdateZoneCommand(id, resource, iamFacade.requireCompanyId());
        return WarehouseHttpResponseMapper.toZoneResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete zone")
    @PreAuthorize("hasAuthority('warehouses.update')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return WarehouseHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteWarehouseZoneCommand(iamFacade.requireCompanyId(), id)), messages);
    }
}
