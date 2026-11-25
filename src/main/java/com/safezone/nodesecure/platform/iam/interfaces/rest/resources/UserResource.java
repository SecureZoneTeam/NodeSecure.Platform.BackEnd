package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "User of the organization")
public record UserResource(
        @Schema(example = "2") Long id,
        @Schema(example = "mrojas") String username,
        @Schema(example = "maria.rojas@andina.pe") String email,
        @Schema(example = "María Rojas Salas") String fullName,
        @Schema(example = "ROLE_OPERATOR") String role,
        @Schema(example = "[\"ROLE_OPERATOR\"]") List<String> roles,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED"}) String status,
        @Schema(example = "1") Long companyId,
        @Schema(example = "[1]") List<Long> warehouseIds,
        @Schema(example = "1", nullable = true) Long warehouseId,
        @Schema(example = "{\"inventory.exit.create\": false}") Map<String, Boolean> permissionOverrides,
        @Schema(example = "[\"inventory.view\"]") List<String> permissions,
        Instant createdAt) {
}
