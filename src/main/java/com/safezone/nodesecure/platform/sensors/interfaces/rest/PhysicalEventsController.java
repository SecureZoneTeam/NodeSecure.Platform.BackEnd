package com.safezone.nodesecure.platform.sensors.interfaces.rest;

import com.safezone.nodesecure.platform.sensors.application.commandservices.SensorCommandService;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.ReceivePhysicalEventResource;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.transform.SensorCommandMapper;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.transform.SensorHttpResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/physical-events", produces = "application/json")
@Tag(name = "Physical IoT Events", description = "Payload reception from ESP32 devices")
public class PhysicalEventsController {

    private final SensorCommandService commandService;
    private final MessageSource messages;

    public PhysicalEventsController(SensorCommandService commandService, MessageSource messages) {
        this.commandService = commandService;
        this.messages = messages;
    }

    @Operation(summary = "Receive a physical event from a registered IoT sensor node",
            description = "Requires the X-Device-Api-Key header. Payload fields accept mac/state/timestamp aliases.")
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> receive(
            @Parameter(description = "Shared device integration key", required = true, name = "X-Device-Api-Key")
            @RequestHeader("X-Device-Api-Key") String ignoredHeader,
            @Valid @RequestBody ReceivePhysicalEventResource resource) {
        var command = SensorCommandMapper.toReceiveEventCommand(resource);
        return SensorHttpResponseMapper.toEventResponse(commandService.handle(command), HttpStatus.CREATED, messages);
    }
}
