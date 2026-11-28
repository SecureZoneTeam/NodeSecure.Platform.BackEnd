package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

/** Resources currently used by an organization, compared against the limits of a plan. */
public record OrganizationUsage(long warehouses, long sensorNodes, long users) {
}
