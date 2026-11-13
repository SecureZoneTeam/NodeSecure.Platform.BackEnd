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

@RestController
@RequestMapping(value = "/api/v1/warehouse-zones", produces = "application/json")
@Tag(name = "Warehouse Zones", description = "Operating zones and authorized shifts")
public class WarehouseZonesController {

    private static final String READ_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER','WAREHOUSE_KEEPER','AUDITOR')";
    private static final String MANAGE_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER')";

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

    @Operation(summary = "Get all zones belonging to the authenticated company")
    @PreAuthorize(READ_ROLES)
    @GetMapping
    public ResponseEntity<List<WarehouseZoneResource>> getAll() {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && warehouseScope == null) {
            return ResponseEntity.ok(List.of());
        }
        List<WarehouseZoneResource> resources = queryService.handle(
                        new GetAllWarehouseZonesQuery(iamFacade.currentCompanyId(), warehouseScope))
                .stream().map(WarehouseResourceMapper::toZoneResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get a zone by id")
    @PreAuthorize(READ_ROLES)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && warehouseScope == null) {
            return WarehouseHttpResponseMapper.notFound("warehouseZone", id, messages);
        }
        return queryService.handle(new GetWarehouseZoneByIdQuery(id, iamFacade.currentCompanyId(), warehouseScope))
                .<ResponseEntity<?>>map(zone -> ResponseEntity.ok(WarehouseResourceMapper.toZoneResource(zone)))
                .orElseGet(() -> WarehouseHttpResponseMapper.notFound("warehouseZone", id, messages));
    }

    @Operation(summary = "Create a zone within one of the company's warehouses")
    @PreAuthorize(MANAGE_ROLES)
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateWarehouseZoneResource resource) {
        var command = WarehouseCommandMapper.toCreateZoneCommand(resource, iamFacade.currentCompanyId());
        return WarehouseHttpResponseMapper.toZoneResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update a warehouse zone and its operating shift")
    @PreAuthorize(MANAGE_ROLES)
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseZoneResource resource) {
        var command = WarehouseCommandMapper.toUpdateZoneCommand(id, resource, iamFacade.currentCompanyId());
        return WarehouseHttpResponseMapper.toZoneResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete a warehouse zone")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return WarehouseHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteWarehouseZoneCommand(iamFacade.currentCompanyId(), id)), messages);
    }
    private Long warehouseKeeperScope() {
        return iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") ? iamFacade.currentWarehouseId() : null;
    }
}
