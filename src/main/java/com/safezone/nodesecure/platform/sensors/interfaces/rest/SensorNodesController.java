package com.safezone.nodesecure.platform.sensors.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.sensors.application.commands.*;
import com.safezone.nodesecure.platform.sensors.application.commandservices.SensorCommandService;
import com.safezone.nodesecure.platform.sensors.application.queries.*;
import com.safezone.nodesecure.platform.sensors.application.queryservices.SensorQueryService;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.CreateSensorNodeResource;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.UpdateSensorNodeResource;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.transform.SensorCommandMapper;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.transform.SensorHttpResponseMapper;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.transform.SensorResourceMapper;
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
@RequestMapping(value = "/api/v1/sensor-nodes", produces = "application/json")
@Tag(name = "IoT Sensor Nodes", description = "ESP32 node registration and connection monitoring")
public class SensorNodesController {

    private static final String READ_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER','WAREHOUSE_KEEPER','AUDITOR')";
    private static final String MANAGE_ROLES = "hasAnyRole('ADMINISTRATOR','SECURITY_MANAGER')";

    private final SensorCommandService commandService;
    private final SensorQueryService queryService;
    private final IamContextFacade iamFacade;
    private final MessageSource messages;

    public SensorNodesController(SensorCommandService commandService, SensorQueryService queryService,
                                 IamContextFacade iamFacade, MessageSource messages) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.messages = messages;
    }

    @Operation(summary = "List sensor nodes for the authenticated company")
    @PreAuthorize(READ_ROLES)
    @GetMapping
    public ResponseEntity<List<?>> getAll() {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && warehouseScope == null) {
            return ResponseEntity.ok(List.of());
        }
        List<?> resources = queryService.handle(new GetAllSensorNodesQuery(iamFacade.currentCompanyId(), warehouseScope)).stream()
                .map(SensorResourceMapper::toResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get a sensor node by id")
    @PreAuthorize(READ_ROLES)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Long warehouseScope = warehouseKeeperScope();
        if (iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") && warehouseScope == null) {
            return SensorHttpResponseMapper.notFound("sensorNode", id, messages);
        }
        return queryService.handle(new GetSensorNodeByIdQuery(id, iamFacade.currentCompanyId(), warehouseScope))
                .<ResponseEntity<?>>map(node -> ResponseEntity.ok(SensorResourceMapper.toResource(node)))
                .orElseGet(() -> SensorHttpResponseMapper.notFound("sensorNode", id, messages));
    }

    @Operation(summary = "Register and link an ESP32 sensor node to a warehouse zone")
    @PreAuthorize(MANAGE_ROLES)
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSensorNodeResource resource) {
        var command = SensorCommandMapper.toRegisterCommand(resource, iamFacade.currentCompanyId());
        return SensorHttpResponseMapper.toSensorResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Change the warehouse zone or configuration of a sensor node")
    @PreAuthorize(MANAGE_ROLES)
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateSensorNodeResource resource) {
        var command = SensorCommandMapper.toUpdateCommand(id, resource, iamFacade.currentCompanyId());
        return SensorHttpResponseMapper.toSensorResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete a sensor node that has no recorded physical events")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return SensorHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteSensorNodeCommand(id, iamFacade.currentCompanyId())), messages);
    }
    private Long warehouseKeeperScope() {
        return iamFacade.currentUserHasRole("ROLE_WAREHOUSE_KEEPER") ? iamFacade.currentWarehouseId() : null;
    }
}
