package com.safezone.nodesecure.platform.warehouses.application.internal.commandservices;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import com.safezone.nodesecure.platform.warehouses.application.commands.*;
import com.safezone.nodesecure.platform.warehouses.application.commandservices.WarehouseCommandFailure;
import com.safezone.nodesecure.platform.warehouses.application.commandservices.WarehouseCommandService;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import com.safezone.nodesecure.platform.warehouses.domain.repositories.WarehouseRepository;
import com.safezone.nodesecure.platform.sensors.interfaces.acl.SensorContextFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Service
public class WarehouseCommandServiceImpl implements WarehouseCommandService {

    private final WarehouseRepository repository;
    private final ApplicationEventPublisher publisher;
    private final SensorContextFacade sensorFacade;
    private final Clock clock;

    public WarehouseCommandServiceImpl(WarehouseRepository repository, ApplicationEventPublisher publisher,
                                       SensorContextFacade sensorFacade, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.sensorFacade = sensorFacade;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Warehouse, WarehouseCommandFailure> handle(CreateWarehouseCommand command) {
        if (command.companyId() == null || command.companyId() <= 0) {
            return Result.failure(new WarehouseCommandFailure.Validation("warehouse.error.company.required"));
        }
        if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
            return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.nameTaken"));
        }
        Warehouse warehouse;
        try {
            warehouse = Warehouse.create(command.companyId(), command.name(), command.address(), clock.instant());
        } catch (IllegalArgumentException exception) {
            return Result.failure(new WarehouseCommandFailure.Validation(exception.getMessage()));
        }
        List<DomainEvent> events = warehouse.pullDomainEvents();
        Warehouse saved = repository.save(warehouse);
        events.forEach(publisher::publishEvent);
        return Result.success(saved);
    }

    @Override
    @Transactional
    public Result<Warehouse, WarehouseCommandFailure> handle(UpdateWarehouseCommand command) {
        Optional<Warehouse> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) return notFound("warehouse", command.id());
        if (repository.existsByCompanyIdAndNameAndIdIsNot(command.companyId(), command.name(), command.id())) {
            return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.nameTaken"));
        }
        try {
            Warehouse warehouse = found.get();
            warehouse.updateInformation(command.name(), command.address(), command.status(), clock.instant());
            return Result.success(repository.save(warehouse));
        } catch (IllegalArgumentException exception) {
            return Result.failure(new WarehouseCommandFailure.Validation(exception.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<Long, WarehouseCommandFailure> handle(DeleteWarehouseCommand command) {
        Optional<Warehouse> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) return notFound("warehouse", command.id());
        if (!found.get().getZones().isEmpty()) {
            return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.deleteHasZones"));
        }
        repository.deleteById(command.id());
        return Result.success(command.id());
    }

    @Override
    @Transactional
    public Result<WarehouseZone, WarehouseCommandFailure> handle(CreateWarehouseZoneCommand command) {
        Optional<Warehouse> found = repository.findByIdAndCompanyId(command.warehouseId(), command.companyId());
        if (found.isEmpty()) return notFound("warehouse", command.warehouseId());
        try {
            Warehouse warehouse = found.get();
            if (warehouse.hasZoneCode(command.code(), null)) {
                return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.zone.codeTaken"));
            }
            warehouse.addZone(command.name(), command.code(), command.riskLevel(), command.operatingShift(), clock.instant());
            List<DomainEvent> events = warehouse.pullDomainEvents();
            Warehouse saved = repository.save(warehouse);
            events.forEach(publisher::publishEvent);
            WarehouseZone zone = saved.getZones().stream()
                    .filter(item -> item.getCode().equalsIgnoreCase(command.code().trim()))
                    .findFirst().orElseThrow();
            return Result.success(zone);
        } catch (IllegalArgumentException exception) {
            return Result.failure(new WarehouseCommandFailure.Validation(exception.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<WarehouseZone, WarehouseCommandFailure> handle(UpdateWarehouseZoneCommand command) {
        Optional<Warehouse> found = repository.findByZoneIdAndCompanyId(command.zoneId(), command.companyId());
        if (found.isEmpty()) return notFound("warehouseZone", command.zoneId());
        try {
            Warehouse warehouse = found.get();
            if (warehouse.hasZoneCode(command.code(), command.zoneId())) {
                return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.zone.codeTaken"));
            }
            WarehouseZone zone = warehouse.updateZone(command.zoneId(), command.name(), command.code(),
                    command.riskLevel(), command.operatingShift(), clock.instant());
            List<DomainEvent> events = warehouse.pullDomainEvents();
            Warehouse saved = repository.save(warehouse);
            events.forEach(publisher::publishEvent);
            return saved.getZones().stream().filter(item -> item.getId().equals(zone.getId())).findFirst()
                    .map(Result::<WarehouseZone, WarehouseCommandFailure>success)
                    .orElseGet(() -> Result.failure(new WarehouseCommandFailure.NotFound("warehouseZone", command.zoneId())));
        } catch (IllegalArgumentException exception) {
            return Result.failure(new WarehouseCommandFailure.Validation(exception.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<Long, WarehouseCommandFailure> handle(DeleteWarehouseZoneCommand command) {
        Optional<Warehouse> found = repository.findByZoneIdAndCompanyId(command.zoneId(), command.companyId());
        if (found.isEmpty()) return notFound("warehouseZone", command.zoneId());
        Warehouse warehouse = found.get();
        if (sensorFacade.zoneHasRegisteredSensors(command.zoneId())) {
            return Result.failure(new WarehouseCommandFailure.Conflict("warehouse.error.zone.deleteHasSensors"));
        }
        if (!warehouse.removeZone(command.zoneId(), clock.instant())) return notFound("warehouseZone", command.zoneId());
        repository.save(warehouse);
        return Result.success(command.zoneId());
    }

    private static <T> Result<T, WarehouseCommandFailure> notFound(String resource, Long id) {
        return Result.failure(new WarehouseCommandFailure.NotFound(resource, id));
    }
}
