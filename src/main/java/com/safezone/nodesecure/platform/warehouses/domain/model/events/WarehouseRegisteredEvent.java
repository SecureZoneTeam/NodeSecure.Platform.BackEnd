package com.safezone.nodesecure.platform.warehouses.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import java.time.Instant;

public record WarehouseRegisteredEvent(Long companyId, String warehouseName, Instant occurredAt)
        implements DomainEvent {
}
