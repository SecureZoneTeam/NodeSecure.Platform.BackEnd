package com.safezone.nodesecure.platform.sensors.application.internal.commandservices;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import com.safezone.nodesecure.platform.sensors.application.commands.*;
import com.safezone.nodesecure.platform.sensors.application.commandservices.SensorCommandFailure;
import com.safezone.nodesecure.platform.sensors.application.commandservices.SensorCommandService;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.MacAddress;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import com.safezone.nodesecure.platform.sensors.domain.repositories.PhysicalEventRepository;
import com.safezone.nodesecure.platform.sensors.domain.repositories.SensorNodeRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SensorCommandServiceImpl implements SensorCommandService {

    private final SensorNodeRepository sensorRepository;
    private final PhysicalEventRepository eventRepository;
    private final WarehouseContextFacade warehouseFacade;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public SensorCommandServiceImpl(SensorNodeRepository sensorRepository, PhysicalEventRepository eventRepository,
                                    WarehouseContextFacade warehouseFacade, ApplicationEventPublisher publisher, Clock clock) {
        this.sensorRepository = sensorRepository;
        this.eventRepository = eventRepository;
        this.warehouseFacade = warehouseFacade;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<SensorNode, SensorCommandFailure> handle(RegisterSensorNodeCommand command) {
        MacAddress mac;
        try { mac = new MacAddress(command.macAddress()); }
        catch (IllegalArgumentException exception) { return validation(exception); }
        if (!warehouseFacade.zoneBelongsToCompany(command.zoneId(), command.companyId())) {
            return Result.failure(new SensorCommandFailure.NotFound("warehouseZone", command.zoneId()));
        }
        if (sensorRepository.existsByMacAddress(mac)) {
            return Result.failure(new SensorCommandFailure.Conflict("sensor.error.macAddressTaken"));
        }
        try {
            SensorNode node = SensorNode.create(command.companyId(), command.zoneId(), mac,
                    command.model(), command.sensorType(), clock.instant());
            List<DomainEvent> events = node.pullDomainEvents();
            SensorNode saved = sensorRepository.save(node);
            events.forEach(publisher::publishEvent);
            return Result.success(saved);
        } catch (IllegalArgumentException exception) {
            return validation(exception);
        }
    }

    @Override
    @Transactional
    public Result<SensorNode, SensorCommandFailure> handle(UpdateSensorNodeCommand command) {
        Optional<SensorNode> found = sensorRepository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) return notFound("sensorNode", command.id());
        if (!warehouseFacade.zoneBelongsToCompany(command.zoneId(), command.companyId())) {
            return Result.failure(new SensorCommandFailure.NotFound("warehouseZone", command.zoneId()));
        }
        MacAddress mac;
        try { mac = new MacAddress(command.macAddress()); }
        catch (IllegalArgumentException exception) { return validation(exception); }
        if (sensorRepository.existsByMacAddressAndIdIsNot(mac, command.id())) {
            return Result.failure(new SensorCommandFailure.Conflict("sensor.error.macAddressTaken"));
        }
        SensorNode node = found.get();
        if (!node.getMacAddress().equals(mac) && eventRepository.existsBySensorNodeId(command.id())) {
            return Result.failure(new SensorCommandFailure.Conflict("sensor.error.macAddress.changeHasEvents"));
        }
        try {
            node.updateConfiguration(command.zoneId(), mac, command.model(), command.sensorType(), clock.instant());
            return Result.success(sensorRepository.save(node));
        } catch (IllegalArgumentException exception) {
            return validation(exception);
        }
    }

    @Override
    @Transactional
    public Result<Long, SensorCommandFailure> handle(DeleteSensorNodeCommand command) {
        Optional<SensorNode> found = sensorRepository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) return notFound("sensorNode", command.id());
        if (eventRepository.existsBySensorNodeId(command.id())) {
            return Result.failure(new SensorCommandFailure.Conflict("sensor.error.deleteHasEvents"));
        }
        sensorRepository.deleteById(command.id());
        return Result.success(command.id());
    }

    @Override
    @Transactional
    public Result<PhysicalEvent, SensorCommandFailure> handle(ReceivePhysicalEventCommand command) {
        MacAddress mac;
        PhysicalEventType eventType;
        try {
            mac = new MacAddress(command.macAddress());
            eventType = PhysicalEventType.from(command.eventType());
        } catch (IllegalArgumentException exception) {
            return validation(exception);
        }
        if (command.occurredAt() == null) {
            return Result.failure(new SensorCommandFailure.Validation("sensor.error.occurredAt.required"));
        }
        Optional<SensorNode> found = sensorRepository.findByMacAddress(mac);
        if (found.isEmpty()) {
            return Result.failure(new SensorCommandFailure.UnregisteredMacAddress(mac.value()));
        }
        SensorNode node = found.get();
        Instant receivedAt = clock.instant();
        if (eventType == PhysicalEventType.NODE_DISCONNECTED) {
            node.markOffline(receivedAt);
        } else {
            node.registerPing(receivedAt);
        }
        SensorNode savedNode = sensorRepository.save(node);
        PhysicalEvent event = PhysicalEvent.create(savedNode.getCompanyId(), savedNode.getId(), savedNode.getZoneId(),
                eventType, command.occurredAt());
        List<DomainEvent> events = event.pullDomainEvents();
        PhysicalEvent savedEvent = eventRepository.save(event);
        events.forEach(publisher::publishEvent);
        return Result.success(savedEvent);
    }

    private static <T> Result<T, SensorCommandFailure> notFound(String resource, Long id) {
        return Result.failure(new SensorCommandFailure.NotFound(resource, id));
    }

    private static <T> Result<T, SensorCommandFailure> validation(IllegalArgumentException exception) {
        return Result.failure(new SensorCommandFailure.Validation(exception.getMessage()));
    }
}
