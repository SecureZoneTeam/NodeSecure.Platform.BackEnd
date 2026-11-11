package com.safezone.nodesecure.platform.iam.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.time.Instant;

public record UserRegisteredEvent(String username, Long companyId, Instant occurredAt) implements DomainEvent {
}
