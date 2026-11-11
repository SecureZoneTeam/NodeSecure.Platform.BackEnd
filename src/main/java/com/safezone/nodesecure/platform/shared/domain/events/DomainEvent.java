package com.safezone.nodesecure.platform.shared.domain.events;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
