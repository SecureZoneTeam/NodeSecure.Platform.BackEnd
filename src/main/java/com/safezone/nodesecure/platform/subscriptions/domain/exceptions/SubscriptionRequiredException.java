package com.safezone.nodesecure.platform.subscriptions.domain.exceptions;

import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.PlanResource;

/** The organization has no active subscription, so it cannot create limited resources (HTTP 409). */
public class SubscriptionRequiredException extends BusinessRuleViolationException {

    public static final String CODE = "SUBSCRIPTION_REQUIRED";

    private final PlanResource resource;

    public SubscriptionRequiredException(PlanResource resource) {
        super("subscriptions.error.required");
        this.resource = resource;
    }

    public PlanResource getResource() {
        return resource;
    }
}
