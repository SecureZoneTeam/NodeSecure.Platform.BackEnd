package com.safezone.nodesecure.platform.subscriptions.interfaces.acl;

/**
 * Plan limits checked by other bounded contexts before they create a limited resource.
 * Each method must run inside the transaction that creates the resource: it locks the
 * organization row so concurrent requests cannot exceed the limit together.
 * Throws a 409 business error when the organization has no active subscription or the
 * limit of its plan is already reached.
 */
public interface SubscriptionContextFacade {

    void requireWarehouseCapacity(Long companyId);

    void requireSensorNodeCapacity(Long companyId);

    void requireUserCapacity(Long companyId);
}
