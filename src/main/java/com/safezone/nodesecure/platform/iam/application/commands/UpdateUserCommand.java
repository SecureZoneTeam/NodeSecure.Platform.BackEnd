package com.safezone.nodesecure.platform.iam.application.commands;

import java.util.List;

/** Null fields keep their current value. */
public record UpdateUserCommand(Long companyId, Long actorUserId, Long id, String email, String fullName,
                                String role, List<Long> warehouseIds) {
}
