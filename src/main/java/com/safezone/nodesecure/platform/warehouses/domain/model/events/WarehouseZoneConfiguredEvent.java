package com.safezone.nodesecure.platform.warehouses.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import java.time.Instant;

/** Published when a zone is created or its operating configuration changes. */
public record WarehouseZoneConfiguredEvent(Long warehouseId, Long zoneId, String zoneCode,
                                           Instant occurredAt) implements DomainEvent {
}
