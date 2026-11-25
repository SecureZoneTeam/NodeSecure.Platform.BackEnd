package com.safezone.nodesecure.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Session context of the signed-in user. Returned by sign-in, sign-up (with token) and GET /me (token null).
 * {@code allWarehouses} is true for the owner; otherwise {@code warehouseIds} lists the assigned warehouses.
 */
@Schema(description = "Signed in user with its organization, role, permissions and warehouses")
public record AuthenticatedUserResource(
        @Schema(example = "1") Long id,
        @Schema(example = "fperez") String username,
        @Schema(example = "Fernando Pérez") String fullName,
        @Schema(example = "fernando.perez@safezone.pe") String email,
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...", nullable = true) String token,
        @Schema(example = "ROLE_OWNER") String role,
        @Schema(example = "[\"ROLE_OWNER\"]") List<String> roles,
        @Schema(example = "[\"inventory.view\"]") List<String> permissions,
        @Schema(example = "true") boolean allWarehouses,
        @Schema(example = "[1, 2]") List<Long> warehouseIds,
        @Schema(example = "1", nullable = true) Long warehouseId,
        @Schema(example = "1") Long organizationId,
        @Schema(example = "Distribuidora Andina S.A.C.") String organizationName) {
}
