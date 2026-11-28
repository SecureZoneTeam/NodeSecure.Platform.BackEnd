package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

/** A resource whose usage does not fit in a plan limit. */
public record LimitViolation(PlanResource resource, long used, int limit) {

    @Override
    public String toString() {
        return resource.key() + " " + used + "/" + limit;
    }
}
