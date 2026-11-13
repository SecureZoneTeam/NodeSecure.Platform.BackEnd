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
import java.util.Objects;

@RestController
@RequestMapping(value = "/api/v1/warehouses", produces = "application/json")
@Tag(name = "Warehouses", description = "Company warehouse infrastructure")
public class WarehousesController {

    private static final String READ_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER','WAREHOUSE_KEEPER','AUDITOR')";
    private static final String MANAGE_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER')";

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

    @Operation(summary = "Get warehouses belonging to the authenticated company")
    @PreAuthorize(READ_ROLES)
    @GetMapping
    public ResponseEntity<List<?>> getAll() {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && warehouseScope == null) {
            return ResponseEntity.ok(List.of());
        }
        List<?> resources = queryService.handle(new GetAllWarehousesQuery(iamFacade.currentCompanyId(), warehouseScope)).stream()
                .map(WarehouseResourceMapper::toResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get a warehouse by id")
    @PreAuthorize(READ_ROLES)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && !Objects.equals(id, warehouseScope)) {
            return WarehouseHttpResponseMapper.notFound("warehouse", id, messages);
        }
        return queryService.handle(new GetWarehouseByIdQuery(id, iamFacade.currentCompanyId(), warehouseScope))
                .<ResponseEntity<?>>map(warehouse -> ResponseEntity.ok(WarehouseResourceMapper.toResource(warehouse)))
                .orElseGet(() -> WarehouseHttpResponseMapper.notFound("warehouse", id, messages));
    }

    @Operation(summary = "Register a warehouse for the authenticated company")
    @PreAuthorize(MANAGE_ROLES)
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateWarehouseResource resource) {
        var command = WarehouseCommandMapper.toCreateCommand(resource, iamFacade.currentCompanyId());
        return WarehouseHttpResponseMapper.toWarehouseResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update a warehouse")
    @PreAuthorize(MANAGE_ROLES)
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseResource resource) {
        var command = WarehouseCommandMapper.toUpdateCommand(id, resource, iamFacade.currentCompanyId());
        return WarehouseHttpResponseMapper.toWarehouseResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete an empty warehouse")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return WarehouseHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteWarehouseCommand(id, iamFacade.currentCompanyId())), messages);
    }
    private Long warehouseKeeperScope() {
        return iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") ? iamFacade.currentWarehouseId() : null;
    }
}
