package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Exceptions to the role defaults: permission code -> true (grant) or false (revoke)")
public record UpdateUserPermissionsResource(
        @Schema(example = "{\"inventory.products.create\": true, \"inventory.exit.create\": false}")
        @NotNull(message = "{iam.error.permission.invalid}")
        Map<String, Boolean> overrides) {
}
