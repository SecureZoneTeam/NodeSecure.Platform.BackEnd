package com.safezone.nodesecure.platform.inventory.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.time.Instant;

public record ProductCreatedEvent(Long companyId, String sku, Instant occurredAt) implements DomainEvent {
}
