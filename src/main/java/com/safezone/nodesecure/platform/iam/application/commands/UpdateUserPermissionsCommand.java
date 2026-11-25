package com.safezone.nodesecure.platform.iam.application.commands;

import java.util.Map;

/** Replaces the permission exceptions of a worker: code -> true (grant) / false (revoke). */
public record UpdateUserPermissionsCommand(Long companyId, Long actorUserId, Long id, Map<String, Boolean> overrides) {
}
