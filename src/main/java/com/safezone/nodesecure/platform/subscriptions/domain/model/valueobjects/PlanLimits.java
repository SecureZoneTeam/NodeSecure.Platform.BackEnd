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

    /** Resources whose current usage does not fit in these limits, e.g. {@code "warehouses 3/1"}. */
    public List<String> violationsFor(OrganizationUsage usage) {
        List<String> violations = new ArrayList<>();
        if (usage.warehouses() > warehouses) violations.add("warehouses " + usage.warehouses() + "/" + warehouses);
        if (usage.sensorNodes() > sensorNodes) violations.add("sensorNodes " + usage.sensorNodes() + "/" + sensorNodes);
        if (usage.users() > users) violations.add("users " + usage.users() + "/" + users);
        return violations;
    }
}
