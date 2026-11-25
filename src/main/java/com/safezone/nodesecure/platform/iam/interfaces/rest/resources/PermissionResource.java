package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Permission of the catalog with the roles that have it by default")
public record PermissionResource(
        @Schema(example = "inventory.entry.create") String code,
        @Schema(description = "Owner-only permissions can never be granted to a worker", example = "false")
        boolean ownerOnly,
        @Schema(example = "[\"ROLE_OWNER\", \"ROLE_SUPERVISOR\", \"ROLE_OPERATOR\"]") List<String> defaultRoles) {
}
