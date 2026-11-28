package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

/** Allowed transitions: ACTIVE -> ACTIVE (plan change) and ACTIVE -> CANCELLED. CANCELLED is final. */
public enum SubscriptionStatus {
    ACTIVE, CANCELLED
}
