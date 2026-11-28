package com.safezone.nodesecure.platform.reporting.application.internal.services;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import com.safezone.nodesecure.platform.reporting.domain.repositories.TraceabilityRecordRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseZoneConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * T29: writes entries of the traceability log. It joins the transaction of the operation that
 * produced the event, so the operation and its record are committed (or rolled back) together.
 * Organization and responsible user come from the event or, when absent, from the server-side session.
 */
@Slf4j
@Service
public class TraceabilityRecorder {

    private final TraceabilityRecordRepository repository;
    private final IamContextFacade iamFacade;
    private final WarehouseContextFacade warehouseFacade;
    private final Clock clock;

    public TraceabilityRecorder(TraceabilityRecordRepository repository, IamContextFacade iamFacade,
                                WarehouseContextFacade warehouseFacade, Clock clock) {
        this.repository = repository;
        this.iamFacade = iamFacade;
        this.warehouseFacade = warehouseFacade;
        this.clock = clock;
    }

    /** What happened; organization, warehouse and actor are completed by {@link #record(Event)}. */
    public record Event(Long companyId, Long warehouseId, Long zoneId, TraceabilityRecordType recordType,
                        String eventType, String entityType, String entityId, String actorFallback,
                        String description, Instant occurredAt) {
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<TraceabilityRecord> record(Event event) {
        Long companyId = event.companyId() != null ? event.companyId() : iamFacade.currentCompanyId();
        if (companyId == null) {
            log.warn("Traceability event {} skipped: no organization could be resolved", event.eventType());
            return Optional.empty();
        }
        Long warehouseId = event.warehouseId();
        if (warehouseId == null && event.zoneId() != null) {
            warehouseId = warehouseFacade.findZoneConfiguration(event.zoneId(), companyId)
                    .map(WarehouseZoneConfiguration::warehouseId).orElse(null);
        }
        if (warehouseId != null && !warehouseFacade.warehouseBelongsToCompany(warehouseId, companyId)) {
            warehouseId = null; // never link a record to a warehouse of another organization
        }
        String sessionUser = iamFacade.currentUsername();
        String actor = sessionUser != null ? sessionUser
                : event.actorFallback() != null ? event.actorFallback() : TraceabilityRecord.SYSTEM_ACTOR;
        Long actorUserId = sessionUser != null ? iamFacade.currentUserId() : null;
        TraceabilityRecord saved = repository.save(TraceabilityRecord.of(new TraceabilityRecord.Entry(
                companyId, warehouseId, event.zoneId(), event.recordType(), event.eventType(), event.entityType(),
                event.entityId(), actor, actorUserId, event.description(), event.occurredAt()), clock.instant()));
        return Optional.of(saved);
    }
}
