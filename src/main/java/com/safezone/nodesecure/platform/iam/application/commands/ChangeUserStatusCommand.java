package com.safezone.nodesecure.platform.iam.application.commands;

/** Suspends ({@code suspend = true}) or reactivates a worker of the organization. */
public record ChangeUserStatusCommand(Long companyId, Long actorUserId, Long id, boolean suspend) {
}
