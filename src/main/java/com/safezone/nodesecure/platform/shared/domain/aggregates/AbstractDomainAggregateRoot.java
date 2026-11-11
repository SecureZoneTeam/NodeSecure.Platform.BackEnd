package com.safezone.nodesecure.platform.shared.domain.aggregates;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDomainAggregateRoot {

    private final List<DomainEvent> events = new ArrayList<>();

    protected final void registerEvent(DomainEvent event) {
        events.add(Objects.requireNonNull(event));
    }

    public final List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copy = List.copyOf(events);
        events.clear();
        return copy;
    }
}
