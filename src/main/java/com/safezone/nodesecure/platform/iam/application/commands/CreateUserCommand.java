package com.safezone.nodesecure.platform.iam.application.commands;

import java.util.List;

/**
 * The owner creates a worker account directly. {@code companyId} and {@code actorUserId}
 * always come from the authenticated session, never from the request body.
 */
public record CreateUserCommand(Long companyId, Long actorUserId, String username, String email, String password,
                                String fullName, String role, List<Long> warehouseIds) {
}
