package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

import java.util.ArrayList;
import java.util.List;

/** Operational quota granted by a plan. Every limit must be greater than zero. */
public record PlanLimits(int warehouses, int sensorNodes, int users) {

    public PlanLimits {
        if (warehouses <= 0 || sensorNodes <= 0 || users <= 0) {
            throw new IllegalArgumentException("subscriptions.error.limits.positive");
        }
    }

    public int of(PlanResource resource) {
        return switch (resource) {
            case WAREHOUSES -> warehouses;
            case SENSOR_NODES -> sensorNodes;
            case USERS -> users;
        };
    }

    /** Resources whose current usage does not fit in these limits (usage greater than the limit). */
    public List<LimitViolation> exceededBy(OrganizationUsage usage) {
        List<LimitViolation> violations = new ArrayList<>();
        for (PlanResource resource : PlanResource.values()) {
            if (usage.of(resource) > of(resource)) {
                violations.add(new LimitViolation(resource, usage.of(resource), of(resource)));
            }
        }
        return violations;
    }

    /** Same as {@link #exceededBy} as text, e.g. {@code "warehouses 3/1"}. */
    public List<String> violationsFor(OrganizationUsage usage) {
        return exceededBy(usage).stream().map(LimitViolation::toString).toList();
    }

    /** True when one more unit of the resource still fits in the limit. */
    public boolean allowsOneMore(PlanResource resource, long used) {
        return used < of(resource);
    }
}
