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

/**
 * IoT nodes of the organization. Reading follows the warehouse scope of the user;
 * registering, configuring and removing devices is reserved to the owner (iot.devices.manage).
 */
@RestController
@RequestMapping(value = "/api/v1/sensor-nodes", produces = "application/json")
@Tag(name = "IoT Sensor Nodes", description = "ESP32 node registration and connection monitoring")
public class SensorNodesController {

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

    @Operation(summary = "List sensor nodes")
    @PreAuthorize("hasAuthority('iot.devices.view')")
    @GetMapping
    public ResponseEntity<List<?>> getAll() {
        List<?> resources = queryService.handle(new GetAllSensorNodesQuery(iamFacade.requireCompanyId(),
                        iamFacade.warehouseScope())).stream()
                .map(SensorResourceMapper::toResource).toList();
        return ResponseEntity.ok(resources);
    }

    @Operation(summary = "Get sensor node by id")
    @PreAuthorize("hasAuthority('iot.devices.view')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return queryService.handle(new GetSensorNodeByIdQuery(id, iamFacade.requireCompanyId(), iamFacade.warehouseScope()))
                .<ResponseEntity<?>>map(node -> ResponseEntity.ok(SensorResourceMapper.toResource(node)))
                .orElseGet(() -> SensorHttpResponseMapper.notFound("sensorNode", id, messages));
    }

    @Operation(summary = "Register sensor node")
    @PreAuthorize("hasAuthority('iot.devices.manage')")
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreateSensorNodeResource resource) {
        var command = SensorCommandMapper.toRegisterCommand(resource, iamFacade.requireCompanyId());
        return SensorHttpResponseMapper.toSensorResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }

    @Operation(summary = "Update sensor node")
    @PreAuthorize("hasAuthority('iot.devices.manage')")
    @PutMapping(value = "/{id}", consumes = "application/json")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateSensorNodeResource resource) {
        var command = SensorCommandMapper.toUpdateCommand(id, resource, iamFacade.requireCompanyId());
        return SensorHttpResponseMapper.toSensorResponse(commandService.handle(command), HttpStatus.OK, messages);
    }

    @Operation(summary = "Delete sensor node")
    @PreAuthorize("hasAuthority('iot.devices.manage')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return SensorHttpResponseMapper.toDeleteResponse(
                commandService.handle(new DeleteSensorNodeCommand(id, iamFacade.requireCompanyId())), messages);
    }
}
